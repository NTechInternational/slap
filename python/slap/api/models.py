#We aren't declaring django models here, we will declare simple classes that will
#act as a model
import random
import string
from api.mongoconn import MongoConnection

class Visitor:
	COLLECTION = MongoConnection.Collections.VISITOR

	class Meta:
		ID_KEY = 'visitorId'
		USER_ID_KEY = 'userId'


	def __init__(self, visitor_id = None, user_id = None):
		"""
		This method instantiates a visitor object by taking visitor_id and
		user_id as optional param
		"""
		self.visitor_id = visitor_id
		self.user_id = user_id

	def save(self):
		"""
		This method saves the visitor model to the database
		If the visitor id exists in the database updates the existing record
		Else the new visitor is saved
		"""
		visitorCollection = MongoConnection().get_collection(self.COLLECTION)

		visitorCollection.replace_one({self.Meta.ID_KEY : self.visitor_id}, 
									self.__to_mongo_representation(),
									upsert = True)

	def __to_mongo_representation(self):
		"""
		converts the object to mongo representation.
		"""
		return {self.Meta.ID_KEY : self.visitor_id, self.Meta.USER_ID_KEY : self.user_id}


	@classmethod
	def create(cls, visitor_id):
		"""
		creates a visitor with provided visitor_id and random user id
		"""
		return Visitor(visitor_id, Visitor.__generate_random_id())


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

		return Visitor(visitor[cls.Meta.ID_KEY], visitor[cls.Meta.USER_ID_KEY])


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

