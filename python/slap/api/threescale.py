import ThreeScalePY
from api.slapsettings import SlapConfig
import logging

class ThreeScale:

    def __init__(self):
        self.authorized = False
        self.description = ''

    def authorize(self, app_id = None, app_key = None):
        """
        Authorizes the app using 3 scale api
        Parameters:
        ----------
        app_id : str
            The id of the 3 scale application that will be used to authorize
        app_key : str
            The key of the 3 scale application
        """
        Config = SlapConfig.ThreeScale;
        if not Config.enabled:
            self.authorized = True
            self.description = '3 scale authorization disabled'
        else:
            #Since threescale authorization is required we perform proper three scale authorization

            provider_key = Config.provider_key
            app_id = Config.app_id if app_id == None else app_id
            app_key = Config.app_key if app_key == None else app_key

            logging.warn('Using provider key %s,  app id %s and app key %s'
                % (provider_key, app_id, app_key))

            client = ThreeScalePY.ThreeScaleAuthRep(provider_key = provider_key,
                app_id = app_id, app_key = app_key)
            if(client.authrep()):
                self.authorized = True
                self.description = 'Authorized by 3 scale'
            else:
                self.authorized = False
                self.description = client.build_response().get_reason()

        return self.authorized
