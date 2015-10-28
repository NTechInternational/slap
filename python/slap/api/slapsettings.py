

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