# GlyTableMaker (backend)
Backend code for the GlyTableMaker application

### If you would like to run the backend application in Eclipse, create a Java run configuration with the following VM argument:

-Djasypt.encryptor.password=<jasypt_secret>

<jasypt_secret> is a password/secret you need to choose and then use it to generate all the encypted passwords in the application.yml file

#### Generating encypted passwords

[PasswordGeneratorUtil](https://github.com/glyspace/glyTableMaker-backend/blob/main/src/main/java/org/glygen/tablemaker/util/PasswordGeneratorUtil.java) should be run with two arguments:

          PasswordGeneratorUtil <my_password> DB

where <my_password> is the passoword you wish to encrypt.

Before running this tool, an environment variable JASYPT_SECRET needs to be set. This secret is the one that the system will use to decrypt the password while running. Therefore, the value used for JASYPT_SECRET when generating the encrypted passwords should be the same as the one that is set as environment variable when GlyTableMaker backend system is running.


#### List of passwords in application.yml that need to be encypted

1. postgres password 
2. google oath: \<client-id> and \<client-secret> need to be encypted
3. gmail api: generate “App” password for the google account that you will be using to send emails. Follow instructions [here](https://support.google.com/search?q=workspace+how+to+generate+an+app+passwords) to generate an App password. The generated password would something like: IOBZ LWTR GRTY WERD

   To use this app password, we need to remove the spaces, then generate the encrypted one using “jasypt” and store that in application.yml.
4. GlyTouCan and GlyCosmos API keys
5. NCBI API key
6. GitHub Token: to be able to create tickets in the github repository

--------------------------------------------------------------------------------------------
### In order to run the application in a Docker container on a server

If this is your first time running the application:
1. Create a folder "glytablemaker" under your user home directory
2. Create docker network

          docker network create glygen-network

To execute glyTablemaker-backend application.

1. set necessary environment variables: You need to replace glygen.ccrc.uga.edu/tablemaker with your own server's address

         export GLYGEN_HOST=glygen.ccrc.uga.edu/tablemaker
   
         export GLYGEN_FRONTEND_HOST=glygen.ccrc.uga.edu/tablemaker

         export GLYGEN_BASEPATH=/api/
   
         export GLYGEN_OAUTH2_REDIRECTURI=https://glygen.ccrc.uga.edu/tablemaker/oauth2/redirect

         export JASYPT_SECRET=<jasypt_secret>  (need to match with the one used for generating the passwords)
    
    make sure environment variable HOME is also set to your user home directory
    
 You can set any of the variables declared in docker-compose.xml file as an environment variable 
 if you need to use values other than defaults provided.
 
 2. Make sure postgres is up and running
 
      to run postgres, go to glyTableMaker-backend/postgres directory
         set necessary environment variables:
         
         export POSTGRES_PASSWORD=<your-password>
         
         docker-compose up -d
      
         
 3. docker-compose up -d
 4. After the backend application is up and running, go to swagger interface: http://localhost:8080/swagger-ui.html (if running locally) or \<server-url\>/api/swagger-ui.html (if on the server). 
--------------------------------------------------------------------------------------------



