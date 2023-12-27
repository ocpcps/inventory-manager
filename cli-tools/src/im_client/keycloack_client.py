"""\
 Handles the Logic of getting a token from KeyCloak
 @Author: Lucas Nishimura <lucas.nishimura at telefonica.com>
"""
import requests
import datetime
from requests.structures import CaseInsensitiveDict


class KeyCloakClient:

    def __init__(self,keyCloakUrl,clientId,clientSecret,certificates,autoLogin=True):
        self.__keyCloakUrl  = keyCloakUrl
        self.__clientSecret = clientSecret
        self.__clientId     = clientId
        self.__certificates = certificates
        #
        #
        #
        if (autoLogin):
            self.login()


    def login(self):
        """
        Login to KeyCloak and get a token, using client credentials
        """
        url = self.__keyCloakUrl + "/protocol/openid-connect/token"
        d = {'client_id': self.__clientId, 'client_secret': self.__clientSecret,'grant_type':'client_credentials','username':'pytsshon-client'}
        response = requests.post(url,timeout=10,verify=False,cert=self.__certificates,data=d)
        response = response.json()
        now      = datetime.datetime.now()
        expire   = now  +  datetime.timedelta(seconds=response['expires_in']-60)
        self.__tokenRefreshDateTime = expire
        self.__token = response['access_token']

    def getToken(self):
        """
        Get a token from KeyCloak and refresh it if needed
        """
        if not hasattr(self, '__tokenRefreshDateTime'):
            self.login()
        now     = datetime.datetime.now()
        if now > self.__tokenRefreshDateTime:
            print("Refresing Token")
            self.login()

        return self.__token
