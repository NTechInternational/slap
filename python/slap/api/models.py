#We aren't declaring django models here, we will declare simple classes that will
#act as a model
import random
import string

class Visitor:
	def __init__(self, visitor_id = None, user_id = None):
		"""
		This method instantiates a visitor object by taking visitor_id and
		user_id as optional param
		"""
		self.visitor_id = visitor_id
		self.user_id = user_id

	@ClassMethod
	def get_by_visitor_id(cls, visitor_id):
		"""
		"""

	@StaticMethod
	def id_generator(size=20, chars=string.ascii_uppercase + string.digits):
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

