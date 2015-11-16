from string import Template

class Transformer:

	@staticmethod
	def convert_answers_to_proper_format(questions):
		"""
		Transform answer in question from 
		"answers": "ChoiceText:Big ticket (realtor or car dealer)| ChoiceFacet:BusinessModel, ... 
		to
		"answers" : [
			{ "ChoiceText" : "Big ticket (realtor or car delear)" },
			{ "ChoiceFacet" : "BusinessModel" },
			...
		]
		"""
		for question in questions:
			answers = question.get('answers')
			formatted_answers = []
			alloptions = answers.split('|') #split options separated by pipe
			i = 0
			for option in alloptions:
				keyvalue = option.split(':', 1)
				i = i + 1
				if len(keyvalue) == 2:
					formatted_answers.append({keyvalue[0].strip() : keyvalue[1].strip()})

			if len(formatted_answers) > 0:
				question['answers'] = formatted_answers

	@staticmethod
	def convert_items_to_proper_format(items):
		"""
		Transform itemtemplate in items/challenges from
		"itemtemplate" : [Beginning &StartTime and ending] [by &Deadline] [be one of the first &RewardLimit people to] become a new &Customer [when you &OfferAction &OfferingAmount &TeaserOffering] [and &Activity,] and we will provide a coupon [for &RewardAmount &Reward] [to &Beneficiary].
		"""
		print(items)

class CTemplate(Template):
	delimiter = '&'
	idpattern = r'[\w\d]+'

class ItemTransformer:

	def __init__(self, items):
		self.items = items

	def transform(self, answered_variables = None):
		"""
		Transforms individual item template based on default and answered variables
		"""
		for item in self.items:
			item['variables'] = self.__get_defaults(item['variables'])
			answered_variables = item['variables']
			self.__fill_values(item, answered_variables)
			self.__sentence_case(item)


	def __sentence_case(self, item):
		"""
		"""

	def __fill_values(self, item, answered_variables):
		"""
		Each item template consists of 
		variables text prefixed by '&'
		and optional blocks enclosed in '[' ']'
		E.g.: [Beginning &StartTime and ending] [by &Deadline] &OfferAction &OfferingAmount &Offering [and &Activity,] and &Reward] [to &Beneficiary].
		So in a more generic terms
		A template consists of
		<template> ::= <word>|<optional statement>|<variable>|<punctuation>
		<variable> ::= &<word>
		<optional statement> = [<word>|<variable>|<punctuation>
		<punctuation> :== <dot>|<comma>
		"""
		#discover all variables
		itemtemplate = item['itemtemplate']

		item['itemtemplate'] = CTemplate(itemtemplate).safe_substitute(answered_variables)

		#remove any variables in optional string
		item['itemtemplate']



	def __get_defaults(self, default_variables):
		"""
		This method extracts the default variable values provided from Solr
		&Customer:customer (Become a new customer and we will provide a coupon.)", "Facets_s": "BusinessModel:All, Goal:NewCustomer, RewardType:Coupon
		
		As seen the variable is a comma separated and is name:value pair
		"""
		variables = {}
		for variable in default_variables.split(','):
			key_value = variable.split(':')
			variables[key_value[0].strip(' &')] = key_value[1].strip()

		return variables

