from pymongo import MongoClient
from pymongo.errors import InvalidName
import logging
from api.slapsettings import SlapConfig


class MongoConnection:
    class Collections:
        VISITOR = "visitors"
        QUESTION = "questions"
        VISITOR_SESSION = "visitorSession"
        VISITOR_SESSION_START_OVER = "visitorSessionStartOver"

    MONGO_DB_NAME = "slap"
    logging.info('Connecting to mongo at %s:%d' % (SlapConfig.Mongo.server_address, SlapConfig.Mongo.server_port))
    mongo_client = MongoClient()

    def store_in_collection(self, collection_name, data):
        try:
            collection = self.get_collection(collection_name)
        except Exception as e:
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
            collection_exists = db.collection_names(False).index(collection_name) >= 0
        except ValueError as e:
            #if it isn't in the database ignore the error
            logging.info('Ignoring ValueError because if collection doesn\'t exist we might have to create one')

        if collection_exists == True:
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
        #Consider using dictionary, right now the method is simple enough for if..elif
        if collection_name == self.Collections.QUESTION:
            #collection.create_index()
            logging.info('Setting up indices for %s collection.' % collection_name)
        elif collection_name == self.Collections.VISITOR:
            #collection.create_index()
            logging.info('Setting up indices for %s collection.' % collection_name)

        logging.warn('Indices for the collection have not been setup properly' +
            'We will have to set it up some day')
        return collection



