# Money transfer
A toy application created for learning purposes.

Implements money transfers between accounts.

Building & running
------------
Requirements: **Java 11**
```
$ git clone https://github.com/a-konyaev/mt.git
$ cd mt
$ mvn package
$ java -jar mt.jar
```

REST API
------------
Base URL: http://localhost:8081/api/

All endpoints use GET method

**Create new account**
   
    /new
Response: ```{"status":"OK","accountId":"<created account id>","balance":"0.00"}```

**Get all accounts**

    /list

Response: ```{"status":"OK","accountIds":[<accounts id array>]}```

**Get account balance**

    /balance?accountId={id}

Response: ```{"status":"OK","accountId":"<requested accont id>","balance":"<amount with 2 decimal digits>"}```

**Put money to account**

    /put?accountId={account id to which put the money}&amount={value from 0.01 to 1000000000.00 with max 2 decimal digits}

Response: ```{"status":"OK"}```

**Get money from account**

    /withdraw?accountId={account id from which get the money}&amount={money amount}

Response: ```{"status":"OK"}```

**Transfer money from one account to another**

    /transfer?accountIdTo={account id from which get the money}&accountIdFrom={account id to which put the money}&amount={money amount}

Response: ```{"status":"OK"}```

#####*ERROR response*
If the API method got an error for some reason, the response will contain:

```{"status":"ERROR","message":"<error reason description>"}```
