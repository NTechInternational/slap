from api.models import Visitor, Error, SlapResponse
import pdb
from api.solr import SolrQuery
from api.transformers import Transformer, ItemTransformer

class Interaction:
	VISITOR_PARAM_KEY = 'visitorid'
	APP_ID_PARAM_KEY = 'appid'
	APP_KEY_PARAM_KEY = 'appkey'
	INTERACTION_PARAM_KEY = 'type'
	CHALLENGE_PARAM_KEY = 'itemid'

	def __init__(self, request = None, auth_provider = None):
		self.request = request
		self.user = None
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
			self.response = Error(auth_provider.description)

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
				self.response = Error('Submit interaction in progress')
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
		if self.user.selected_challenge_id != None:
			self.select()
		else:
			pass
		self.response = Error('Default interaction in progress')

	def select(self):
		if self.CHALLENGE_PARAM_KEY in self.request.GET:
			challenge_id = self.request.GET[self.CHALLENGE_PARAM_KEY]
			(success, challenge_id) = self.__parse_int(challenge_id)
			if success:
				self.user.select_challenge(challenge_id)
				self.response = SlapResponse(self.user)
				questions = SolrQuery(query_params = {'rows' : 1}).query()
				Transformer.convert_answers_to_proper_format(questions)
				self.response.set_questions(questions)
				items = SolrQuery(query_type = SolrQuery.CHALLENGE_QUERY, query_params = {'rows' : 5}).query()
				ItemTransformer(items = items).transform()
				self.response.set_items(items)
			else:
				self.has_error = True
				self.response = Error('Invalid challenge was selected')



	def __parse_int(self, string_value):
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
		if self.response != None:
			return self.response.to_json()

		return Error('Nothing has been initialized').to_json()
