#We aren't declaring django models here, we will declare simple classes that will
#act as a model
import random
import string
import logging
from api.mongoconn import MongoConnection

class Visitor:
	COLLECTION = MongoConnection.Collections.VISITOR

	class Meta:
		ID_KEY = 'visitorId'
		USER_ID_KEY = 'userId'
		SELECTED_CHALLENGE_KEY = 'selectedChallenge'


	def __init__(self, visitor_id = None, user_id = None, selected_challenge_id = None):
		"""
		This method instantiates a visitor object by taking visitor_id and
		user_id as optional param
		"""
		self.visitor_id = visitor_id
		self.user_id = user_id
		self.selected_challenge_id = selected_challenge_id



	def save(self):
		"""
		This method saves the visitor model to the database
		If the visitor id exists in the database updates the existing record
		Else the new visitor is saved
		"""
		visitorCollection = MongoConnection().get_collection(self.COLLECTION)

		visitorCollection.replace_one({self.Meta.USER_ID_KEY : self.user_id}, 
									self.to_json(),
									upsert = True)

	def select_challenge(self, challenge_id):
		"""
		selects a new challenge for a given user
		"""
		self.selected_challenge_id = challenge_id
		self.save()

	def to_json(self):
		"""
		converts the object to mongo representation.
		"""
		return {self.Meta.ID_KEY : self.visitor_id,
				 self.Meta.USER_ID_KEY : self.user_id,
				 self.Meta.SELECTED_CHALLENGE_KEY : self.selected_challenge_id}


	@classmethod
	def create(cls, user_id):
		"""
		creates a visitor with provided user id and random visitor id
		"""
		return Visitor(user_id = user_id, visitor_id = Visitor.__generate_random_id())


	@classmethod
	def get_by_user_id(cls, user_id):
		"""
		returns the visitor with a specific user id
		"""
		return cls.__get_visitor_with({ cls.Meta.USER_ID_KEY : user_id})

	@classmethod
	def get_by_visitor_id(cls, visitor_id):
		"""
		returns the visitor with a particular id
		"""
		return cls.__get_visitor_with({cls.Meta.ID_KEY : visitor_id});
		
	@classmethod
	def __get_visitor_with(cls, filter):
		"""
		internal method that matches finds a matching record based on the filter.
		the filter could be any object type filter
		Parameters:
		filter - object
			The filter that should be used to select from the visitor collection
		"""
		visitor = MongoConnection().get_collection(cls.COLLECTION).find_one(filter)

		if visitor is None:
			logging.info('Couldn\'t find record with filter ')
			return None

		logging.info('Match found')
		return Visitor(visitor_id = visitor[cls.Meta.ID_KEY],
		 				user_id = visitor[cls.Meta.USER_ID_KEY],
		 				selected_challenge_id = visitor[cls.Meta.SELECTED_CHALLENGE_KEY])


	@staticmethod
	def __generate_random_id(size=40, chars=string.ascii_uppercase + string.digits):
		"""
		This method generates a unique id string of size with chars.
		From: http://stackoverflow.com/questions/12179271/python-classmethod-and-staticmethod-for-beginner
		Parameters:
		-----------
		size - int
			The size of the string to be returned

		chars - char array
			The string containing the list of valid characters to include
		"""
		return ''.join(random.SystemRandom().choice(chars) for _ in range(size))


class Error:
	class Meta:
		ERROR_KEY = 'errorDescription'

	def __init__(self, error_description = ''):
		"""
		Initializes an error object
		"""
		self.error_description = error_description

	def to_json(self):
		return { self.Meta.ERROR_KEY : self.error_description }


class SlapResponse:

	class Meta:
		ID_KEY = 'visitorId'
		ITEMS_KEY = 'items'
		QUESTIONS_KEY = 'questions'

	def __init__(self, visitor, questions = None, items = None):
		"""
		"""
		self.visitor = visitor
		self.questions = questions
		self.items = items

	def set_questions(self, questions):
		self.questions = questions

	def set_items(self, items):
		self.items = items

	def to_json(self):
		ret_val = {self.Meta.ID_KEY : self.visitor.visitor_id}
		if self.items != None:
			ret_val[self.Meta.ITEMS_KEY] = self.items

		if self.questions != None:
			ret_val[self.Meta.QUESTIONS_KEY] = self.questions

		return ret_val
