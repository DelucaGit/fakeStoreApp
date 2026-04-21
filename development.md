# Development Log

# 19 April 2026
* I have created two separate services inside one monolithic repository in Github.
* I have created two separate databases in PostgreSQL 18, one for users/orders and one for products. 
* I have created a separate database user to manage the two databases. Both databases share the same user and although this is not best praxis in a production environment - keep in mind that this is a small school project. 
* The new database user has limited permissions. This is because I don't want to use the standard root user "postgre" because I know myself and will probably delete the whole database. The user permissions are limited to: 
    * CONNECT - Make a connection to the database.
    * USAGE - Look inside the database. 
    * CREATE - To create new tables. I am still thinking about this permission. I had to activate it because Flyway couldn't create its init otherwise, but I will probably work around this so that the user can't create new tables by himself.
    * Standard CRUD-operations (Select, insert, Update & Delete). Delete is limited to rows only. 
    * User can not use DROP, which is used to delete the whole table or the whole database.
    * User can not use ALTER, which is used to change the structure of the tables or change the name of a column or change data type.
* These restrictions are based on the "Least Privilige" approach, meaning that users should only have access to what they absolutely need to. 
* I ran into some issues setting up Flyway in my InteliJ environment. Turned out that I need an extra dependency in my pom.xml that specifies a specifik flyway-version for PostgreSQL. I found the solution on Linkedin, thanks for nothing Gemini. 

Link to the post: linkedin.com/posts/nicholas-ocket_it-turns-out-that-flyway-and-postgres-dont-activity-7374795973887434752-NOnN 

* I've followed the standard "Database First" approach, meaning I've set up my database and tables before writing my classes on Java. 
* Before setting up my databases, I've designed how the table structures will look like. The design was made in Draw.io and the files are availale in the repository.

# 20 April 2026
* Started branch feature/user-registration to work on the logic behind user registration.
* I implemented Java Records for DTO's instead of traditional Java classes to make sure they are not vulnurable to change.
* UserRegistrationRequest takes in email, name & password.
* UserResponse sends back data but excludes sensible data like passwords.
* I've configured SecurityConfig to have a stateless session policy in order to prepare it for JWT.
* All endpoints inside SecurityConfig have been hidden using @Value and environment variables. The endpoints in the controller remain visible for debugging purpose.
* I've opened up the endpoint for registration but I've blocked all other endpoints from incoming traffic.
* For the StoreUser class I've used UUID for the user id. This is to make it harder for hackers to guess the id of an user.
* The function that fetches the user data uses the user id to fetch it, instead of the user email because the user email might contain first name and last name and that data will be sent through the URL bar and might be stored in the browsers history. An UUID is harder to understand who it might belong to.
* Although we use the user email to check if another user has the same email when registering.
* User password is hashed using BCrypt and the hashed version is saved in the database.

# 21 April 2026
* Added a test layer on UserService.
* Ran test on the user registration function and user fetching function.
* Tests were succesful. 
