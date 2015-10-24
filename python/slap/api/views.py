from django.shortcuts import render
from django.http import JsonResponse
from api import models


def get_visitor_id(request):
	"""
	returns the visitor for a given user id. if invalid user id is provided
	with the appropriate error message
	"""
	visitor_response = 'Unitialized'
	user_id = request.GET.get('userid', '')
	if not user_id.strip():
		#throw an error
		visitor_response = models.Error('Please provide a valid user id').to_json()
	else:
		visitor = models.Visitor.get_by_user_id(user_id)
		if not visitor:
			visitor = models.Visitor.create(user_id)
			visitor.save()

		visitor_response = visitor.to_json()

	return JsonResponse(visitor_response)

def process_request(request):
	"""
	processes the visitors interaction request
	"""
	return JsonResponse(models.Error('Work in progress').to_json())
