from api.slapsettings import SlapConfig
from urllib import parse, request
import logging
import json


class SolrQuery:
    QUESTION_QUERY = 'question'
    CHALLENGE_QUERY = 'challenge'
    FILTER_SEPARATOR = ':'
    QUERY_FILTER_KEY = 'fq'
    FILTER_KEY = 'q'
    PATH_KEY = 'path'

    def __init__(self, query_type=QUESTION_QUERY, query_params={}):
        self.query_type = query_type
        self.query_params = query_params
        self.__config = SlapConfig.Solr

    def query(self):
        """
        This method queries the SOLR server and returns the result
        """
        query_url = self.get_query_url()
        logging.info('Querying: ' + query_url)
        json_data = request.urlopen(query_url).read().decode()
        logging.debug('Retrieved the following ' + json_data)
        response = json.loads(json_data)

        return self.get_docs_from_response(response)

    def get_docs_from_response(self, svr_response):
        docs = svr_response

        # iterate through the json and get the docs
        for path in self.__config.solr_response_map[self.PATH_KEY]:
            docs = docs[path]

        # change the docs key to json key
        for doc in docs:
            keys = list(doc.keys())
            for key in keys:
                tkey = self.__get_app_key_from_solr_key(key)
                doc[tkey[1]] = dict.pop(doc, key)

        return docs

    def get_query_url(self):
        """
        This method returns the actual url that must be queried to resolve the request
        """
        url = self.__config.server_url
        url = url + self.__config.paths[self.query_type] + '?'
        logging.debug('The url to query is %s' % url)

        params = {
            self.FILTER_KEY: {},
            self.QUERY_FILTER_KEY: {}
        }

        # add passed params
        self.__add_query_param(self.query_params, params)

        # add default params if they have been passed they will not overwrite previously passed values
        self.__add_query_param(self.__config.default_params, params, False)

        # add static params such as wt=json
        self.__add_query_param(self.__config.params, params)

        # add the items in filter query to filter params
        url = url + '&' + self.__get_filter_param(params[self.FILTER_KEY])
        url = url + '&' + self.__get_query_filter_param(params[self.QUERY_FILTER_KEY])

        del params[self.FILTER_KEY]
        del params[self.QUERY_FILTER_KEY]

        url = url + '&' + parse.urlencode(params)

        return url

    def __add_query_param(self, params_to_map, params, overwrite=True):
        """
        adds the passed params to to the list of query params map
        :param params_to_map: the params to add to the final list
        :param params: the output params
        :param overwrite: if set to true overwrites any existing param, else ignores it
        """
        for key, value in params_to_map.items():
            (is_query_param, tkey) = self.__get_solr_key_from_app_key(key)
            if is_query_param:

                if not overwrite:
                    # check if the param is already present, if it is don't overwrite
                    # since the param can have aliases or equivalents, treat equivalent as present
                    aliases = [tkey]
                    for group in self.__config.equivalent_params:
                        if key in self.__config.equivalent_params[group]:
                            aliases = self.__config.equivalent_params[group]
                            break

                    present_in_map = False
                    for alias in aliases:
                        (is_qp, akey) = self.__get_solr_key_from_app_key(alias)

                        if is_qp and (akey in params[self.QUERY_FILTER_KEY] or akey in params[self.FILTER_KEY]):
                            present_in_map = True

                    # don't add this param move on to next param since it is present in the map
                    if present_in_map:
                        continue

                # query params are filter query params if they are present in filter_query_params list
                if tkey in self.__config.filter_query_params:
                    params[self.QUERY_FILTER_KEY][tkey] = value
                else:
                    params[self.FILTER_KEY][tkey] = value
            else:
                params[tkey] = value

    def __get_filter_param(self, params):
        url = ''
        for key, value in params.items():
            string_value = value
            if isinstance(value, Filter):
                string_value = value.to_string()
            url = url + (key + self.FILTER_SEPARATOR + string_value + ' AND ')
            # TODO: add support for range parameters

        if url.endswith('AND '):
            url = url[:-4]

        if not url:
            url = '*:*'

        url = parse.urlencode({self.FILTER_KEY: url})
        return url

    def __get_query_filter_param(self, params):
        url = ''
        for key, value in params.items():
            string_value = value
            if isinstance(value, Filter):
                string_value = value.to_string()

            url = url + parse.urlencode({self.QUERY_FILTER_KEY: (key + self.FILTER_SEPARATOR + string_value)}) + '&'

        return url

    def __get_solr_key_from_app_key(self, key):
        """
        internal method that returns the name of the solr parameter given the apps
        parameter name. Returns the mapped key if mapping is found else the default value
        Returned value is a tuple with first value 
        Parameters
        ----------
        key - string
            The name of the parameter to be mapped
        """
        keys = self.__config.get_inverse_map()
        if key in keys:
            return (True, keys[key])

        return (False, key)

    def __get_app_key_from_solr_key(self, key):
        """
        internal method that returns the name of the app parameter given the solr
        parameter name. Returns the mapped key if mapping is found else the default value
        Returned value is a tuple with first value 
        Parameters
        ----------
        key - string
            The name of the parameter to be mapped
        """
        keys = self.__config.solr_response_map['map']
        if key in keys:
            return (True, keys[key])

        return (False, key)


class Filter:
    """
    Represents a complex filter value, that will be passed to SOLR for query
    """

    COMBINATION_OP_AND = ' AND '
    COMBINATION_OP_OR = ' OR '

    def __init__(self, values, combination_operator=COMBINATION_OP_OR, ):
        self.combination_operator = combination_operator
        self.values = values

    def to_string(self):
        """
        Converts the filter to string representation
        :return: returns the string equivalent of the filter
        """
        filter_string = '('
        variable_length = len(self.values) - 1
        for index in range(0, variable_length + 1):
            filter_string += '&' + self.values[index]
            # append or for all but the last
            if index < variable_length:
                filter_string += self.combination_operator
        filter_string += ')'

        return filter_string
