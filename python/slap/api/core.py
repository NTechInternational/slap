from api.models import Visitor, Error, SlapResponse, Question, InteractionLog, VisitorSessionInfo
import pdb
from api.solr import SolrQuery, Filter
from api.transformers import Transformer, ItemTransformer


class Interaction:
    """

    """

    VISITOR_PARAM_KEY = 'visitorid'
    APP_ID_PARAM_KEY = 'appid'
    APP_KEY_PARAM_KEY = 'appkey'
    INTERACTION_PARAM_KEY = 'type'
    CHALLENGE_PARAM_KEY = 'itemid'
    VARIABLE_PARAM_KEY = 'var'
    FACETS_PARAM_KEY = 'facet'
    QUESTION_PARAM_KEY = 'questionid'
    FINAL_TEXT_PARAM_KEY = 'text'

    class Interactions:
        SELECT = 'select'
        SUBMIT = 'submit'
        DONE = 'done'
        START_OVER = 'startover'
        BACK = 'back'

    def __init__(self, request = None, auth_provider = None):
        self.request = request
        self.user = Visitor()
        self.response = SlapResponse(self.user)
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
            if self.user is not None:
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

            if interaction == self.Interactions.SELECT:
                self.select_interaction()
            elif interaction == self.Interactions.SUBMIT:
                self.submit_interaction()
            elif interaction == self.Interactions.DONE:
                self.done_interaction()
            elif interaction == self.Interactions.START_OVER:
                self.start_over_interaction()
            elif interaction == self.Interactions.BACK:
                self.back_interaction()
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
            self.select_interaction()
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

    def select_interaction(self):
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
                # save the old_challenge_id so that we can save it in interaction log
                old_challenge_id = self.user.selected_challenge_id

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


                # e. Return response
                Transformer.convert_answers_to_proper_format(questions)
                self.response.set_questions(questions)

                # Log the current interaction
                self._push_interaction(self.Interactions.SELECT, {self.CHALLENGE_PARAM_KEY : old_challenge_id})
            else:
                self.has_error = True
                self.response = Error('Invalid challenge was selected')

    def submit_interaction(self):
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

            # Old submission will be stored incase we need to undo
            old_submission = Question.get_submissions_for(self.user.visitor_id, question_id)
            old_submission = old_submission[0].to_json() if len(old_submission) == 1 else None

            if self.VARIABLE_PARAM_KEY in getRequest:
                variables = self.__get_name_value_from_string(getRequest[self.VARIABLE_PARAM_KEY])
                has_submission = True # a user could have submitted either a variable's value or facets

            if self.FACETS_PARAM_KEY in getRequest:
                facets = self.__get_name_value_from_string(self.request.GET[self.FACETS_PARAM_KEY])
                has_submission = True

            # there is no point saving if nothing has been submitted or selected
            if has_submission:
                self.user.save_submission(question_id, variables, facets)
                self._push_interaction(self.Interactions.SUBMIT, {self.QUESTION_PARAM_KEY: question_id,
                                                              'submission': old_submission})


            answered_variables = self.user.get_answered_variables()
            selected_facets = self.user.get_selected_facets()

            # TODO: Code Refactor, too many duplicates, move code Question and Item Query block to a common method

            self.response = SlapResponse(visitor = self.user)

            item_id = None

            if self.CHALLENGE_PARAM_KEY in getRequest:
                challenge_id = getRequest[self.CHALLENGE_PARAM_KEY]
                (success, challenge_id) = self.__parse_int(challenge_id)
                if success:
                    item_id = challenge_id

            if item_id is not None:
                items = SolrQuery(query_type = SolrQuery.CHALLENGE_QUERY,
                                  query_params = {'rows' : 1, 'id' : str(item_id), 'businessmodel' : '*'}).query()
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

    def start_over_interaction(self):
        """
        Resets the current session and starts a new one
        :return:
        """
        # collection = InteractionLog.get_collection()
        #
        # log = InteractionLog.get_interaction(self.user.visitor_id, collection)
        # # log.push('asd', 'Hello', collection)
        # last_interaction = log.pop(collection)
        self.user.reset_session()
        self.default_interaction()

    def done_interaction(self):
        text = ''
        if self.FINAL_TEXT_PARAM_KEY in self.request.GET:
            text = self.request.GET[self.FINAL_TEXT_PARAM_KEY]

        # TODO: consider the case where new variables and facets are submitted too.

        self.user.save_session()

        self.default_interaction()

    def back_interaction(self):
        Meta = InteractionLog.Meta
        last_interaction = self._pop_interaction()
        if last_interaction is not None:
            interaction_type = last_interaction[Meta.INTERACTION_TYPE_KEY]
            if interaction_type == self.Interactions.SUBMIT:
                # undo submit interaction
                question_id = last_interaction[InteractionLog.Meta.OLD_VALUE_KEY][self.QUESTION_PARAM_KEY]
                submission = last_interaction[InteractionLog.Meta.OLD_VALUE_KEY]['submission']

                if submission is None:
                    self.user.delete_submission(question_id)
                else:
                    self.user.save_submission(question_id,
                                              submission[Question.Meta.VARIABLES_KEY],
                                              submission[Question.Meta.FACETS_KEY])
            elif interaction_type == self.Interactions.SELECT:
                # undo select interaction
                self.user.select_challenge(last_interaction[InteractionLog.Meta.OLD_VALUE_KEY][self.CHALLENGE_PARAM_KEY])

        #perform default interaction
        self.default_interaction()

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

    def _push_interaction(self, type, old_value):
        collection = InteractionLog.get_collection()
        log = InteractionLog.get_interaction(self.user.visitor_id, collection)
        log.push(type, old_value, collection)

    def _pop_interaction(self):
        collection = InteractionLog.get_collection()
        log = InteractionLog.get_interaction(self.user.visitor_id, collection)

        return log.pop(collection)