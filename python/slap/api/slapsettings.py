import logging

class SlapConfig:
    #Configuration for mongodb for connecting using pymongo
    class Mongo:
        server_address = 'localhost'
        server_port = 27017
    

    #Configuration for 3 scale, default keys
    class ThreeScale:
        provider_key = '3 scale provider key'
        app_id = '3 scale app id'
        app_key = '3 scale app key'
        enabled = False


    #Configuration for SOLR
    class Solr:
        server_url = 'http://localhost:8983'
        paths = {
            'question' : '/solr/collection1/questions',
            'challenge' : '/solr/collection1/items'
        }
        # SOLR Query configuration parameters
        # set the key value pair in the params dictionary, if the value
        # should always be sent
        params = {
            'wt' : 'json'
        }
        # set the key value pair in default params, if it is overridable
        default_params = {
            'language' : 'en',
            'sort' : 'priority_i desc',
            'rows' : '5',
            'businessmodel' : 'All'
        }

        # SOLR supports two type of query one standard query other filter query
        # Filter Query don't affect the score
        # Add the filter query params if
        filter_query_params = ['language_s', 'BusinessModel_ss', 'Goal_ss', 'Motivation_ss', 'RewardType_ss']

        # equivalent params stores the params that must be treated as the same
        # for instance business model, reward type and goal all 'Facet' types
        # so presence of any one type should remove the default value
        equivalent_params = {
            'facet_params' : ['businessmodel' , 'rewardtype', 'goal', 'motivation']
        }


        # Solr response map, maps the solr type to webservice type
        solr_response_map = {
            'path' : ['response', 'docs'], #this is an ordered path so is equivalent to response.docs
            'map' : {
                'BusinessModel_ss' : 'businessmodel',
                'Goal_ss' : 'goal',
                'Motivation_ss' : 'motivation',
                'id' : 'id',
                'source' : 'sourcetype', #values would be questions/item note:item is same as challenge
                'question_t' : 'question',
                'priority_i' : 'priority',
                'Group_i' : 'group',
                'type_s' : 'type',
                'answers_t' : 'answers',
                'Variables_s' : 'variables',
                'Template_t' : 'itemtemplate',
                'RewardType_ss' : 'rewardtype',
                'language_s' : 'language'
            } 
        }

        # inverse map of response map
        __solr_inv_map = {}

        @classmethod
        def get_inverse_map(cls):
            """
            This method returns the inverse map of response map. Inverse map
            can be useful for changing the request to solr response
            """
            if len(cls.__solr_inv_map) == 0:
                #cache the inverse map, from response map because we don't want
                #manual creation of inverse map
                logging.info('Creating inverse map')
                for key, value in cls.solr_response_map['map'].items():
                    cls.__solr_inv_map[value] = key #invert the key to value

            return cls.__solr_inv_map;

