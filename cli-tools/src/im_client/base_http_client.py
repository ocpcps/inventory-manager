import requests
from requests.structures import CaseInsensitiveDict
from .keycloack_client import KeyCloakClient

class BaseHttpClient:

    def __init__(self,baseUrl,authToken,keyCloakClient:KeyCloakClient):
        self.__authToken = authToken
        self.__headers   = CaseInsensitiveDict()
        self.__baseUrl   = baseUrl
        self.__headers["x-auth-token"] = authToken
        self.__headers["Accept"]       = "application/json"
        self.__keyCloak = keyCloakClient


    def __handleResponse(self,response,payLoad=None):
        response.close()
        try:
            if response and response.status_code < 300:
                response = response.json()
                if response['statusCode'] == 200:
                    if 'payLoad' in response:
                        return response['payLoad']
                else:
                    print("ERROR ON Request Please Check")
                    if payLoad:
                        print(payLoad)
                    return response
            else:
                print("ERROR ON Request Please Check")
                if payLoad:
                        print(payLoad)
                print("Response:")
                print(response.content)
                return response.json()
        except:
            print("ERROR")
            print(response.status_code)
            print(response.content)

    def put(self,url,payLoad):        
        self.__headers["Authorization"] = 'Bearer ' + self.__keyCloak.getToken()
        url = self.__baseUrl + url
        print(payLoad)
        response = requests.put(url, json=payLoad,headers=self.__headers,timeout=10,verify=False)
        return self.__handleResponse(response,payLoad)

    def get(self,url):
        self.__headers["Authorization"] = 'Bearer ' + self.__keyCloak.getToken()
        url = self.__baseUrl + url
        response = requests.get(url,headers=self.__headers,timeout=10,verify=False)
        return self.__handleResponse(response)

    def post(self,url,payLoad=None):
        self.__headers["Authorization"] = 'Bearer ' + self.__keyCloak.getToken()
        url = self.__baseUrl + url
        response = requests.post(url, json=payLoad,headers=self.__headers,timeout=10,verify=False)
        # print(response.content)
        return self.__handleResponse(response)
        
    def delete(self,url,payLoad=None):
        self.__headers["Authorization"] = 'Bearer ' + self.__keyCloak.getToken()
        url         = self.__baseUrl + url
        if payLoad:
            response    = requests.delete(url,json=payLoad,headers=self.__headers,timeout=10,verify=False)
            return self.__handleResponse(response)
        else:
            response    = requests.delete(url,headers=self.__headers,timeout=10,verify=False)
            return self.__handleResponse(response)