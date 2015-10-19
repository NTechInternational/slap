from pymongo import MongoClient
from pymongo.errors import InvalidName
import logging


class MongoConnection:
	class Collections:
		VISITOR = "visitors"
		QUESTION = "questions"
		VISITOR = "visitorSession"
		VISITOR_SESSION_START_OVER = "visitorSessionStartOver"
		TEMP_QUESTION = "tempQuestions" #looks like temp question and challenge isn't required
		TEMP_CHALLENGE = "tempChallenges"
		
	MONGO_DB_NAME = "slap"
	mongo_client = MongoClient()
	print(mongo_client)

	def store_in_collection(self, collection_name, data):
		try:
			collection = self.get_collection(collection_name)
			
		except Exception, e:
			raise e

	def get_collection(self, collection_name):
		"""
		Returns the collection specified by the collection_name, if the
		collection doesn't already exist a collection is created
		Parameters:
		----------
		collection_name : str
				the name of the mongodb collection to get
		"""
		db = self.get_db()
		collection_exists = False

		try:
			collection_exists = db.collection_names(false)
									.index(collection_name) >= 0
			 
		except ValueError, e:
			#if it isn't in the database ignore the error

		if collection_exists:
			return db[collection_name]
		else:
			#create a collection and set the indices
			logging.info('Creating the collection \'%s\'for first time' % collection_name)
			return self.__setup_indices(db[collection_name], collection_name)
			

		
	def get_db(self):
		"""
		Returns the default database to use in the application
		"""
		return self.mongo_client.get_database(self.MONGO_DB_NAME)

	def __setup_indices(self, collection, collection_name):
		if(collection_name == self.QUESTION_COLLECTION_NAME)



