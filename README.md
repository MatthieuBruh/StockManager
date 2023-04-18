
<h1 align="center">STOCK MANAGER</h1>
<h2 align="center">Matthieu Brühwiler - Spring 2023 - V.1.0.0</h2>
<br>

---

## Table of Contents
1. [Introduction](#introduction)
2. [Requirements](#requirements)
3. [Installation](#installation)
4. [Usage](#usage)

---
<br/>

<a name="introduction"></a>
## Introduction
Stock Manager is a Java Spring application that allows you to manage: product stocks, supplier and customer orders, and your employees.

It has been developed in the context of the Bachelor's thesis of Matthieu Brühwiler at the [University of Applied Sciences Haaga-Helia](https://www.haaga-helia.fi/en) in the spring of 2023.

This application is a starting point for a stock management system. It is possible to use it like this, but it is recommended to adapt it to your needs. You are free to modify the code as you wish, however, you must respect the license.

The application is a backend application that uses a MariaDB database. It is possible to interact with the system using the provided REST API. The author highly recommends using a frontend application to interact with the system. For the moment, the author has not developed a frontend application, but it is planned to do so in the future.

**The author is not responsible for any damage caused by the use of this application.**

<a name="requirements"></a>
## Requirements
The application is using the Java Spring 3.0.0.RELEASE framework. In order to run the application, you need to have the following components:
* Java JDK **17** or higher.
* MariaDB **10.10.0** or higher.
* Apache Maven **4.0.0** or higher.
* A web server that supports Jakarta EE. (For example: Apache Tomcat 10.0.0 or higher.)

*It is possible that the application works with another version of the components, but it has not been tested.*

<a name="installation"></a>
## Installation
*This installation tutorial is based for an Apache Tomcat server on a Windows operating system. The adaptation to another server or operating system is left to the user.*

As stated in the requirements, you need to have a web server that supports Jakarta EE. For this tutorial, we will use an Apache Tomcat server 11.0.0. The installation of the server is not covered in this tutorial. You can find the installation instructions on the [Apache Tomcat website](https://tomcat.apache.org/download-11.cgi).

To run the project, you need to specify various information to the Spring Framework.
The Spring framework requires a properties file. 

### 1. Specify the environments variables to Apache Tomcat
In order to specify the environment variables to Apache Tomcat, you need to create a file named `stockmanager.xml` in the `[TOMCAT_INSTALLATION]/conf/Catalina/localhost`. This file should contain the following content:

```xml
<?xml version='1.0' encoding='utf-8'?>
<Context>
    <Environment name="spring.config.location" value="file:PATH/TO/PROPERTIES/FILE/stockManager.properties" type="java.lang.String"/>
</Context>
```

### 2. Create the properties file
The Spring application requires you to provide information about the used profile, the database connection, the CORS properties, and the JWT properties. This file should be located in the `PATH/TO/PROPERTIES/FILE` specified in the previous step.

Below is an **example** of a properties file:

```properties
# Profile (dev or prod)
spring.profiles.active=prod

# Database properties
spring.datasource.url=jdbc:mariadb://localhost:3306/stockmanagement
spring.datasource.username=root
spring.datasource.password=
spring.sql.init.mode=always
spring.jpa.defer-datasource-initialization=true
spring.datasource.driver-class-name=org.mariadb.jdbc.Driver
spring.jpa.generate-ddl=true
spring.jpa.hibernate.ddl-auto=update

# Cors properties
spring.security.cors.allowed-origins=http://localhost:3000
spring.security.cors.allowed-methods=GET,POST,PUT,DELETE
spring.security.cors.allowed-headers=Authorization,Content-Type

# JWT properties
jwt.expiration.duration=8
jwt.expiration.unit=10
jwt.secret=THIS_IS_A_BAD_SECRET
```

### 3. WAR file of the application
To deploy the application on Apache Tomcat, you need to create a WAR file. As written in the requirements, the project is based on the Maven framework. By this fact, to create the WAR file, you need to execute the following commands:

```shell
mvn clean package
mvn install
```

After creating the WAR file named `stockmanager.war`, you need to copy it in the `[TOMCAT_INSTALLATION]/webapps` folder.


### 4. Start the server
At this point, you can now start the server. The Spring application manage by itself the database schema automatically. It will create, update, and delete the tables as needed.

To run the Apache Tomcat server, you need to execute the `catalina.bat` file located in the `[TOMCAT_INSTALLATION]/bin` folder.

The application should be available at the following URL: `http://IP_ADDRESS:8080/stockmanager/api`.

<a name="usage"></a>
## Usage
This chapter is an overview of the different endpoints of the application.

Before reading details about the endpoints, all of them have four commons http status codes:
* **200**: The request has succeeded.
* **400**: The request has failed because of a bad request.
* **401**: The request has failed because the user is not authenticated.
* **500**: The request has failed because of an internal server error.


### 1. Authentication
To access the application, you need to be authenticated. The authentication is based on the JWT token.

<table>
    <tr>
        <th>Endpoint</th>
        <th>Method</th>
        <th>Description</th>
        <th>Request Body</th>
        <th>HTTP Status</th>
    </tr>
    <tr>
        <td>/api/auth/login</td>
        <td>POST</td>
        <td>Authenticate an employee</td>
        <td>
            <table>
                <tr>
                    <th>Field</th>
                    <th>Type</th>
                    <th>Description</th>
                </tr>
                <tr>
                    <td>username</td>
                    <td>String</td>
                    <td>The password of the employee</td>
                </tr>
                <tr>
                    <td>password</td>
                    <td>String</td>
                    <td>The password of the employee</td>
                </tr>
            </table>
        </td>
        <td>
            <table>
                <tr>
                    <th>Status</th>
                    <th>Description</th>
                </tr>
                <tr>
                    <td>401</td>
                    <td>Wrong credentials.</td>
                </tr>
            </table>
        </td>
    </tr>
    <tr>
        <td>/api/auth/password</td>
        <td>PUT</td>
        <td>Change the password of an employee</td>
        <td>
            <table>
                <tr>
                    <th>Field</th>
                    <th>Type</th>
                    <th>Description</th>
                </tr>
                <tr>
                    <td>currentPassword</td>
                    <td>String</td>
                    <td>The current password of the employee</td>
                </tr>
                <tr>
                    <td>newPassword</td>
                    <td>String</td>
                    <td>The new password of the employee</td>
                </tr>
                <tr>
                    <td>newPasswordVerification</td>
                    <td>String</td>
                    <td>The new password of the employee</td>
                </tr>
            </table>
        </td>
        <td>
            <table>
                <tr>
                    <th>Status</th>
                    <th>Description</th>
                </tr>
                <tr>
                    <td>400</td>
                    <td>Current password is incorrect.</td>
                </tr>
                <tr>
                    <td>412</td>
                    <td>New password is invalid, or not the same.</td>
                </tr>
            </table>
        </td>
    </tr>
</table>

### 2. Employees

<table>
    <tr>
        <th>Endpoint</th>
        <th>Method</th>
        <th>Description</th>
        <th>Request Body</th>
        <th>HTTP Status</th>
    </tr>
    <tr>
        <td>/api/employees</td>
        <td>GET</td>
        <td>Get the list of all the employees</td>
        <td></td>
        <td>
            <table>
                <tr>
                    <th>Status</th>
                    <th>Description</th>
                </tr>
                <tr>
                    <td>204</td>
                    <td>No employee found.</td>
                </tr>
            </table>
        </td>
    </tr>
    <tr>
        <td>/api/employees/{id}</td>
        <td>GET</td>
        <td>Get an employee by his/her id.</td>
        <td></td>
        <td>
            <table>
                <tr>
                    <th>Status</th>
                    <th>Description</th>
                </tr>
                <tr>
                    <td>400</td>
                    <td>No employee matches given id.</td>
                </tr>
            </table>
        </td>
    </tr>
    <tr>
        <td>/api/employees</td>
        <td>POST</td>
        <td>Create a new employee.</td>
        <td>
            <table>
                <tr>
                    <th>Field</th>
                    <th>Type</th>
                    <th>Description</th>
                </tr>
                <tr>
                    <td>email</td>
                    <td>String</td>
                    <td>The email address of the employee</td>
                </tr>
                <tr>
                    <td>username</td>
                    <td>String</td>
                    <td>The username of the employee</td>
                </tr>
                <tr>
                    <td>firstName</td>
                    <td>String</td>
                    <td>The first name of the employee</td>
                </tr>
                <tr>
                    <td>lastName</td>
                    <td>String</td>
                    <td>The last name of the employee</td>
                </tr>
                <tr>
                    <td>password</td>
                    <td>String</td>
                    <td>The password of the employee</td>
                </tr>
            </table>
        </td>
        <td>
            <table>
                <tr>
                    <th>Status</th>
                    <th>Description</th>
                </tr>
                <tr>
                    <td>201</td>
                    <td>Employee created.</td>
                </tr>
                <tr>
                    <td>400</td>
                    <td>A value provided is null or empty.</td>
                </tr>
                <tr>
                    <td>409</td>
                    <td>Username or email already exists.</td>
                </tr>
            </table>
        </td>
    </tr>
    <tr>
        <td>/api/employees/{id}</td>
        <td>PUT</td>
        <td>Modify an employee by his/her id.</td>
        <td>
            <table>
                <tr>
                    <th>Field</th>
                    <th>Type</th>
                    <th>Description</th>
                </tr>
                <tr>
                    <td>email</td>
                    <td>String</td>
                    <td>The email address of the employee</td>
                </tr>
                <tr>
                    <td>username</td>
                    <td>String</td>
                    <td>The username of the employee</td>
                </tr>
                <tr>
                    <td>firstName</td>
                    <td>String</td>
                    <td>The first name of the employee</td>
                </tr>
                <tr>
                    <td>lastName</td>
                    <td>String</td>
                    <td>The last name of the employee</td>
                </tr>
            </table>
        </td>
        <td>
            <table>
                <tr>
                    <th>Status</th>
                    <th>Description</th>
                </tr>
                <tr>
                    <td>200</td>
                    <td>Employee modified.</td>
                </tr>
                <tr>
                    <td>400</td>
                    <td>No employee matches the given id.</td>
                </tr>
            </table>
        </td>
    </tr>
    <tr>
        <td>/api/employees/{id}/activate</td>
        <td>PUT</td>
        <td>Activate an employee id</td>
        <td></td>
        <td>
            <table>
                <tr>
                    <th>Status</th>
                    <th>Description</th>
                </tr>
                <tr>
                    <td>200</td>
                    <td>The employee has been modified.</td>
                </tr>
                <tr>
                    <td>400</td>
                    <td>No employee found, or the given email and username does not corresponds.</td>
                </tr>
            </table>
        </td>
    </tr>
    <tr>
        <td>/api/employees/{empId}/add-role/{roleId}</td>
        <td>PUT</td>
        <td>Assign a role to an employee.</td>
        <td></td>
        <td>
            <table>
                <tr>
                    <th>Status</th>
                    <th>Description</th>
                </tr>
                <tr>
                    <td>400</td>
                    <td>Wrong role or employee id.</td>
                </tr>
                <tr>
                    <td>409</td>
                    <td>Role already assigned.</td>
                </tr>
            </table>
        </td>
    </tr>
    <tr>
        <td>/api/employees/{empId}/remove-role/{roleId}</td>
        <td>PUT</td>
        <td>Unassign a role to an employee.</td>
        <td></td>
        <td>
            <table>
                <tr>
                    <th>Status</th>
                    <th>Description</th>
                </tr>
                <tr>
                    <td>400</td>
                    <td>Wrong role or employee id.</td>
                </tr>
                <tr>
                    <td>406</td>
                    <td>Role is not assigned to this employee.</td>
                </tr>
            </table>
        </td>
    </tr>
    <tr>
        <td>/api/employees/{id}</td>
        <td>DELETE</td>
        <td>Deactivate an employee account.</td>
        <td></td>
        <td>
            <table>
                <tr>
                    <th>Status</th>
                    <th>Description</th>
                </tr>
                <tr>
                    <td>400</td>
                    <td>No employee matches the given id.</td>
                </tr>
            </table>
        </td>
    </tr>
</tale>