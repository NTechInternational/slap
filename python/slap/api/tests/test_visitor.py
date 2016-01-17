from django.utils import unittest
from django.test.client import RequestFactory
from api.models import Visitor
from api.views import SlapView
from api.core import Interaction
import json


class TestAPIBase(unittest.TestCase):

    def setUp(self):
        self.factory = RequestFactory()

    def _clean_up_user(self, username):
        user = Visitor.get_by_user_id(username)
        if user is not None:
            user.remove_all()


class TestVisitorRegistration(TestAPIBase):
    """
    This test checks for all user registration api
    i.e. /rest/getvisitorid
    """

    # Convention: Every test will create user starting with their Test Name
    TEST_USER_NAME_1 = 'TestVisitorRegistration-User1'
    TEST_USER_NAME_2 = 'TestVisitorRegistration-User2'

    def setUp(self):
        """
        It clears all the exisiting values in the db and creates an empty one for the database
        """
        super().setUp()
        for user in [self.TEST_USER_NAME_1, self.TEST_USER_NAME_2]:
            self._clean_up_user(user)

    def test_user_id_is_nt_registered(self):

        request = self.factory.get('/api?{0}={1}'.format(SlapView.USER_ID_PARAM_KEY, self.TEST_USER_NAME_1))
        response = SlapView()
        response.get_visitor_id(request)

        assert Visitor.Meta.ID_KEY in response.json_data, 'Response has not been returned correctly'
        assert response.json_data[Visitor.Meta.USER_ID_KEY] == self.TEST_USER_NAME_1, \
            'The user name of response should match to passed value'
        assert response.json_data[Visitor.Meta.SELECTED_CHALLENGE_KEY] is None,\
            'Selected challenge should not be initialized'


if __name__ == '__main__':
    unittest.main()