from api.models import Visitor, Error, SlapResponse, Question
import pdb
from api.solr import SolrQuery, Filter
from api.transformers import Transformer, ItemTransformer


class Interaction:
    VISITOR_PARAM_KEY = 'visitorid'
    APP_ID_PARAM_KEY = 'appid'
    APP_KEY_PARAM_KEY = 'appkey'
    INTERACTION_PARAM_KEY = 'type'
    CHALLENGE_PARAM_KEY = 'itemid'
    VARIABLE_PARAM_KEY = 'var'
    FACETS_PARAM_KEY = 'facet'
    QUESTION_PARAM_KEY = 'questionid'

    def __init__(self, request = None, auth_provider = None):
        self.request = request
        self.user = Visitor()
        self.response = None
        self.has_error = False
        self.auth_provider = auth_provider

    def ensure_user_is_valid(self):
        """
        this method ensures that a valid user has been provided for the Interaction
        returns true if valid user is found else returns false
        """
        if self.VISITOR_PARAM_KEY in self.request.GET:
            visitor_id = self.request.GET[self.VISITOR_PARAM_KEY]
            self.user = Visitor.get_by_visitor_id(visitor_id)
            if self.user != None:
                return True

        self.has_error = True
        self.response = Error('Invalid visitor ID provided')

        return False

    def authenticate(self):
        """
        performs authentication with the given authentication provider
        """
        app_id = None
        app_key = None

        if self.APP_ID_PARAM_KEY in self.request.GET:
            app_id = self.request.GET[self.APP_ID_PARAM_KEY]

        if self.APP_KEY_PARAM_KEY in self.request.GET:
            app_key = self.request.GET[self.APP_KEY_PARAM_KEY]

        if not self.auth_provider.authorize():
            self.response = Error(self.auth_provider.description)

        return self.auth_provider.authorized

    def route(self):
        """
        performs the various interaction based on the parameter passed.
        """
        #  Based on the various submission perform actions
        #  the various interactions could be
        #   &qid=2001&label=customer name&value=customer&qtype=variable&..&visitorId=123&type=submit
        #  "submit" => User answers/submits response to question [Interaction 1]
        #  "select" => User selects a challenge					[Interaction 2]
        #  "done" => User session Done							[Interaction 3]
        #  "startOver" => User session Start over				[Interaction 4]
        if self.INTERACTION_PARAM_KEY in self.request.GET:
            interaction = self.request.GET[self.INTERACTION_PARAM_KEY].lower()
            if interaction == 'select':
                self.select()
            elif interaction == 'submit':
                self.submit()
            elif interaction == 'done':
                self.response = Error('Done interaction in progress')
            elif interaction == 'startover':
                self.response = Error('StartOver interaction in progress')
            elif interaction == 'back':
                self.response = Error('Back interaction in progress')
            else:
                self.default_interaction()
        else:
            self.default_interaction()

    def default_interaction(self):
        """
        default interaction
        """
        if self.user.selected_challenge_id != None:
            #Challenge is selected so we need to switch to challenge interaction
            self.select()
        else:
            #check if facets were previously submitted
            #if they have been we select based on the facet
            #else get a single query response
            self.response = SlapResponse(self.user)
            questions = SolrQuery(query_params = {'rows' : 1}).query()
            Transformer.convert_answers_to_proper_format(questions)
            self.response.set_questions(questions)
            items = SolrQuery(query_type = SolrQuery.CHALLENGE_QUERY, query_params = {'rows' : 1}).query()
            Transformer.convert_items_to_proper_format(items)
            self.response.set_items(items)

    def select(self):
        """
        select interaction that selects an item/challenge
        Identifies the variables that don't have a value i.e. a user hasn't provided a value or default doesn't exist
        It then queries the server to get the questions with the missing variables so that the user can provide the
        correct answer
        """
        if self.CHALLENGE_PARAM_KEY in self.request.GET:
            challenge_id = self.request.GET[self.CHALLENGE_PARAM_KEY]
            (success, challenge_id) = self.__parse_int(challenge_id)
            if success:
                self.response = SlapResponse(self.user)
                # a. save the selected transaction in selectedChallenge
                self.user.select_challenge(challenge_id)

                # b. query the database to get the missing variables in the challenge (item)
                #   Note: we are sending business facet to '*' because we need to select the challenge without the facet
                items = SolrQuery(query_type = SolrQuery.CHALLENGE_QUERY,
                                  query_params = {'rows' : 1, 'id' : str(challenge_id), 'businessmodel' : '*'}).query()
                answered_variables = self.user.get_answered_variables()
                ItemTransformer(items = items).transform(answered_variables = answered_variables)
                self.response.set_items(items)

                # c & d. query the database for question with the missing variables
                #       query the database for question with default variables
                # since we are selecting only one item we can select the variables with missing and default value for it
                variables_lists = [items[0]['missingVariables'], items[0]['defaultsOnly']]
                questions = []

                for missing_vars in variables_lists:
                    query_params = {}

                    if len(missing_vars) > 0:
                        for var in missing_vars:
                            var = '&' + var
                        query_params['variables'] = Filter(missing_vars)

                    questions.extend(SolrQuery(query_params = query_params).query())


                # e. return response
                Transformer.convert_answers_to_proper_format(questions)
                self.response.set_questions(questions)

            else:
                self.has_error = True
                self.response = Error('Invalid challenge was selected')

    def submit(self):
        """
        Submit interaction stores the given submission provided by the user. Submission can be answer to a
        particular variable or selection of a facet.
        """
        getRequest = self.request.GET

        if self.QUESTION_PARAM_KEY in getRequest:
            question_id = getRequest[self.QUESTION_PARAM_KEY]
            variables = {}
            facets = {}
            has_submission = False

            if self.VARIABLE_PARAM_KEY in getRequest:
                variables = self.__get_name_value_from_string(getRequest[self.VARIABLE_PARAM_KEY])
                has_submission = True # a user could have submitted either a variable's value or facets

            if self.FACETS_PARAM_KEY in getRequest:
                facets = self.__get_name_value_from_string(self.request.GET[self.FACETS_PARAM_KEY])
                has_submission = True

            # there is no point saving if nothing has been submitted or selected
            if has_submission:
                self.user.save_submission(question_id, variables, facets)

            answered_variables = self.user.get_answered_variables()
            selected_facets = self.user.get_selected_facets()

            # TODO: Code Refactor, too many duplicates, move code Question and Item Query block to a common method
            # a lot of code such as getting the challenge and question is duplicate

            self.response = SlapResponse(visitor = self.user)

            item_id = None

            if self.CHALLENGE_PARAM_KEY in getRequest:
                challenge_id = getRequest[self.CHALLENGE_PARAM_KEY]
                (success, challenge_id) = self.__parse_int(challenge_id)
                if success:
                    item_id = challenge_id

            if item_id is not None:
                items = SolrQuery(query_type = SolrQuery.CHALLENGE_QUERY,
                                  query_params = {'rows' : 1, 'id' : str(challenge_id), 'businessmodel' : '*'}).query()
                answered_variables = self.user.get_answered_variables()
                ItemTransformer(items = items).transform(answered_variables = answered_variables)
                self.response.set_items(items)

                # c & d. query the database for question with the missing variables
                #       query the database for question with default variables
                # since we are selecting only one item we can select the variables with missing and default value for it
                variables_lists = [items[0]['missingVariables'], items[0]['defaultsOnly']]
                questions = []

                for missing_vars in variables_lists:
                    query_params = {}

                    if len(missing_vars) > 0:
                        for var in missing_vars:
                            var = '&' + var
                        query_params['variables'] = Filter(missing_vars)

                    questions.extend(SolrQuery(query_params = query_params).query())
                    if len(questions) > 1:
                        questions = questions[:1] # select only one question when item id is provided
                        break

                Transformer.convert_answers_to_proper_format(questions)
                self.response.set_questions(questions)
            else:
                items = SolrQuery(query_type = SolrQuery.CHALLENGE_QUERY,
                                  query_params = selected_facets).query()
                ItemTransformer(items = items).transform(answered_variables = answered_variables)
                self.response.set_items(items)

                questions = SolrQuery().query() # fetch default questions
                Transformer.convert_answers_to_proper_format(questions)
                self.response.set_questions(questions)

    def __get_name_value_from_string(self, variable = ''):
        """
        parses the string where name value are submitted as
        <Variable Name>:<Variable value>&var=<Variable Name>:<Variable value>...

        i.e. list of key value separated by ampersand (&), where each individual key and value are separated by colon (:)
        :param variable: the string containing the values
        :return: returns the list of values that have been submitted for the question
        """
        KEY_VALUE_PAIR_SEPARATOR = '&'
        KEY_VALUE_SEPARATOR = ':'
        ret_value = {}

        if variable is not None :
            # split the key-value pairs separated by &
            pairs = variable.split(KEY_VALUE_PAIR_SEPARATOR)

            for pair in pairs:
                key_value = pair.split(KEY_VALUE_SEPARATOR)
                if len(key_value) == 2:
                    ret_value[key_value[0]] = key_value[1]

        return ret_value

    def __parse_int(self, string_value):
        """
            returns a tuple with (success/failure, parsed integer value) i.e. first value indicates whether a valid integer
            value was found or not and second value contains the parsed value.
        """
        retVal = None
        try:
            retVal = (True, int(float(string_value)))
        except Exception as e:
            retVal = (False, 0)

        return retVal

    def response_to_json(self):
        """
        returns the json response
        """
        if self.response is not None:
            return self.response.to_json()

        return Error('Nothing has been initialized').to_json()


