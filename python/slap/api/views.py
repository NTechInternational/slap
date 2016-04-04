from django.shortcuts import render
from django.http import HttpResponse
from api import models
from api.core import Interaction
from api.threescale import ThreeScale
import json as simplejson

class SlapView:
    USER_ID_PARAM_KEY = 'userid'
    CALLBACK_PARAM_KEY = 'callback'

    def __init__(self):
        self.json_data = {}

    def _format_response(self, response, request):

        self.json_data = response

        try:
            data = simplejson.dumps(response)
            if 'callback' in request.REQUEST:
                # a jsonp response!
                data = '%s(%s);' % (request.REQUEST['callback'], data)
                return HttpResponse(data, "text/javascript")
        except:
            data = simplejson.dumps(str(response))

        return HttpResponse(data, "application/json")

    def get_visitor_id(self, request):
        """
        returns the visitor for a given user id. if invalid user id is provided
        with the appropriate error message
        """

        user_id = request.GET.get(self.USER_ID_PARAM_KEY, '')
        if not user_id.strip():
            # throw an error
            visitor_response = models.Error('Please provide a valid user id').to_json()
        else:
            visitor = models.Visitor.get_by_user_id(user_id)
            if not visitor:
                visitor = models.Visitor.create(user_id)
                visitor.save()

            visitor_response = visitor.to_json()

        return self._format_response(visitor_response, request)

    def process_request(self, request):
        """
        processes the visitors interaction request
        """
        # determine the type of interaction
        interaction = Interaction(request, auth_provider = ThreeScale())

        if interaction.authenticate():
            if interaction.ensure_user_is_valid():
                # perform valid interaction
                interaction.route()

        return self._format_response(interaction.response_to_json(), request)
