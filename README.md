
<h1 align="center">STOCK MANAGEMENT SYSTEM</h1>
<h2 align="center">Matthieu Brühwiler - Spring 2023 - V.1.0.0</h2>
<br>

---

## Table of Contents
1. [Introduction](#introduction)
2. [Requirements](#requirements)
3. [Installation](#installation)
    1. [Specify the environments variables to Apache Tomcat](#spec-env-variables)
    2. [Create the properties file](#properties-file)
    3. [WAR file of the application](#war-file)
    4. [Start the server](#start-server)
4. [Usage](#usage)
    1. [Authentication](#authentication)
    2. [Employees](#employees)
    3. [Geolocations](#geolocations)
    4. [Customers](#customers)
    5. [Suppliers](#suppliers)
    6. [Brands](#brands)
    7. [Categories](#categories)
    8. [Products](#products)


---
<br/>

<a name="introduction"></a>
## 1. Introduction
Stock Manager is a Java Spring application that allows you to manage: product stocks, supplier and customer orders, and your employees.

It was developed in the context of the [Bachelor's thesis of Matthieu Brühwiler](https://urn.fi/URN:NBN:fi:amk-2023051912121) at the [University of Applied Sciences Haaga-Helia](https://www.haaga-helia.fi/en) in the spring of 2023.

This application is a starting point for a stock management system. It is possible to use it like this, but adapting it to your needs is recommended. You are free to modify the code as you wish. However, you must respect the license.

The application is a backend application that uses a MariaDB database. It is possible to interact with the system using the provided REST API. The author highly recommends using a front-end application to interact with the system. For the moment, the author has not developed a front-end application, but it is planned to do so in the future.

---
---
<br/>

<a name="requirements"></a>
## 2. Requirements
The application uses Java Spring 3.0.0.RELEASE framework. In order to run the application, you need to have the following components:
* Java JDK **17** or higher,
* MariaDB **10.10.0** or higher,
* Apache Maven **4.0.0** or higher,
* A web server that supports Jakarta EE. (For example, Apache Tomcat 10.0.0 or higher.)

*The application may work with another version of the components, but it has not been tested.*

---
---
<br/>

<a name="installation"></a>
## 3. Installation
*This installation tutorial is based on an Apache Tomcat server on a Windows operating system. The adaptation to another server or operating system is left to the user.*

As stated in the requirements, you must have a web server that supports Jakarta EE. For this tutorial, we will use an Apache Tomcat server 11.0.0. The installation of the server is not covered in this tutorial. You can find the installation instructions on the [Apache Tomcat website](https://tomcat.apache.org/download-11.cgi).

To run the project, you must specify various information to the Spring Framework in a properties file.

<a name="spec-env-variables"></a>
### 3.1 Specify the environment variables to Apache Tomcat.
In order to specify the environment variables to Apache Tomcat, you need to create a file named `stockmanager.xml` in the `[TOMCAT_INSTALLATION]/conf/Catalina/localhost`. This file should contain the following content:

```xml
<?xml version='1.0' encoding='utf-8'?>
<Context>
    <Environment name="spring.config.location" value="file:PATH/TO/PROPERTIES/FILE/stockManager.properties" type="java.lang.String"/>
</Context>
```

<a name="properties-file"></a>
### 3.2 Create the properties file
The Spring application requires you to provide information about the profile, the database connection, the CORS properties, and the JWT properties. This file should be located in the `PATH/TO/PROPERTIES/FILE` specified in the previous step.

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

<a name="war-file"></a>
### 3.3 WAR file of the application
To deploy the application on Apache Tomcat, you need to create a WAR file. As written in the requirements, the project is based on the Maven framework. Of this fact, to create the WAR file, you need to execute the following commands:

```shell
mvn clean package
mvn install
```

* The application contains several tests, and the duration of the tests can take 10 minutes. It is important not to skip the tests when creating the WAR file. If you skip the tests, the application may not work properly.*

After creating the WAR file named `stockmanager.war`, you need to copy it into the `[`TOMCAT_INSTALLATION]/webapps` folder.

<a name="start-server"></a>
### 3.4 Start the server
At this point, you can now start the server. The Spring application manages by itself the database schema automatically. It will create, update, and delete the tables as needed.

To run the Apache Tomcat server, you must execute the `catalina.bat` file located in the `[TOMCAT_INSTALLATION]/bin` folder.

The application should be available at the following URL: `http://IP_ADDRESS:8080/stockmanager/api`.

---
---
<br/>

<a name="usage"></a>
## 4. Usage
This chapter is an overview of the different endpoints of the application.

Before reading details about the endpoints, all of them have four common HTTP status codes:
* **200 / 201**: The request has succeeded.
* **400**: The request has failed because of a bad request.
* **401**: The request has failed because the employee is not authenticated.
* **500**: The request has failed because of an internal server error.

However, in the case of an error, whatever the status code is, the server always logs the error and returns a JSON object explaining the error.

Before using the application, you should consider the following properties:
* Some endpoints can be accessed depending on the employee's role. 
* The application roles are:
    * **ROLE_ADMIN**: id= 1
    * **ROLE_MANAGER**: id= 2
    * **ROLE_EMPLOYEE**: id= 3
* The application has a default admin user with the following credentials:
    * **username**: main
    * **password**: A1234
    * :warning: **This employee should be deactivated after the first connection.**

It is important to note that most endpoints that return a list have paging, searching, and sorting methods.
As in the following example: **/api/brands?page=0&size=100&sort=name,asc&searchQuery=B**.
* page: The page number.
* size: The number of elements per page.
* sort: The sorting method. The first parameter is the field to sort, and the second parameter is the sorting order (asc or desc).
* searchQuery: The search query.

<br>

<a name="authentication"></a>
### 4.1 Authentication
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

---

<a name="employees"></a>
### 4.2 Employees
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
                    <td>No employee found, or the given email and username do not correspond.</td>
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
</table>


#### 4.2.1 Employee statistics
<table>
    <tr>
        <th>Endpoint</th>
        <th>Method</th>
        <th>Description</th>
        <th>Request Body</th>
        <th>HTTP Status</th>
    </tr>
    <tr>
        <td>/api/statistics/suppliers?date={optionalDate}</td>
        <td>GET</td>
        <td>Get general statistics about suppliers.</td>
        <td></td>
        <td>
            <table>
                <tr>
                    <th>Status</th>
                    <th>Description</th>
                </tr>
                <tr>
                    <td>204</td>
                    <td>No suppliers found.</td>
                </tr>
            </table>
        </td>
    </tr>
    <tr>
        <td>/api/statistics/customers?date={optionalDate}</td>
        <td>GET</td>
        <td>Get general statistics about customers.</td>
        <td></td>
        <td>
            <table>
                <tr>
                    <th>Status</th>
                    <th>Description</th>
                </tr>
                <tr>
                    <td>204</td>
                    <td>No customers found.</td>
                </tr>
            </table>
        </td>
    </tr>
    <tr>
        <td>/api/statistics/employee={empId}?date={optionalDate}</td>
        <td>GET</td>
        <td>Calculate statistics for an employee on a defined month and year.</td>
        <td></td>
        <td>
            <table>
                <tr>
                    <th>Status</th>
                    <th>Description</th>
                </tr>
                <tr>
                    <td>204</td>
                    <td>No orders related to this employee were found.</td>
                </tr>
                <tr>
                    <td>400</td>
                    <td>Wrong employee id.</td>
                </tr>
            </table>
        </td>
    </tr>
    <tr>
        <td>/api/statistics/stock-to-sale-ratio?date={optionalDate}</td>
        <td>GET</td>
        <td>Calculate the stock-to-sale ratio.</td>
        <td></td>
        <td>
            <table>
                <tr>
                    <th>Status</th>
                    <th>Description</th>
                </tr>
                <tr>
                    <td>204</td>
                    <td>No customer orders found.</td>
                </tr>
            </table>
        </td>
    </tr>
    <tr>
        <td>/api/statistics/sell-through-rate?date={optionalDate}</td>
        <td>GET</td>
        <td>Calculate the sell-through rate.</td>
        <td></td>
        <td>
            <table>
                <tr>
                    <th>Status</th>
                    <th>Description</th>
                </tr>
                <tr>
                    <td>204</td>
                    <td>No customer or supplier orders found.</td>
                </tr>
                <tr>
                    <td>412</td>
                    <td>No product was received during this period.</td>
                </tr>
            </table>
        </td>
    </tr>
    <tr>
        <td>/api/statistics/stock-outs?date={optionalDate}</td>
        <td>GET</td>
        <td>Calculate the stock out rate.</td>
        <td></td>
        <td>
            <table>
                <tr>
                    <th>Status</th>
                    <th>Description</th>
                </tr>
                <tr>
                    <td>204</td>
                    <td>No product found.</td>
                </tr>
                <tr>
                    <td>412</td>
                    <td>No available product for this period.</td>
                </tr>
            </table>
        </td>
    </tr>
    <tr>
        <td>/api/statistics/service-level</td>
        <td>GET</td>
        <td>Calculate the service level.</td>
        <td></td>
        <td>
            <table>
                <tr>
                    <th>Status</th>
                    <th>Description</th>
                </tr>
                <tr>
                    <td>204</td>
                    <td>No customer or supplier order.</td>
                </tr>
            </table>
        </td>
    </tr>
</table>

---

<a name="geolocations"></a>
### 4.3 Geolocations
<table>
    <tr>
        <th>Endpoint</th>
        <th>Method</th>
        <th>Description</th>
        <th>Request Body</th>
        <th>HTTP Status</th>
    </tr>
    <tr>
        <td>/api/geolocations</td>
        <td>GET</td>
        <td>Get all the geolocations.</td>
        <td></td>
        <td>
            <table>
                <tr>
                    <th>Status</th>
                    <th>Description</th>
                </tr>
                <tr>
                    <td>204</td>
                    <td>No geolocation found.</td>
                </tr>
            </table>
        </td>
    </tr>
    <tr>
        <td>/api/geolocations/{id}</td>
        <td>GET</td>
        <td>Get a geolocations by its id.</td>
        <td></td>
        <td>
            <table>
                <tr>
                    <th>Status</th>
                    <th>Description</th>
                </tr>
                <tr>
                    <td>400</td>
                    <td>No geolocation matches with the given id.</td>
                </tr>
            </table>
        </td>
    </tr>
    <tr>
        <td>/api/geolocations</td>
        <td>POST</td>
        <td>Create a new geolocation.</td>
        <td>
            <table>
                <tr>
                    <th>Field</th>
                    <th>Type</th>
                    <th>Description</th>
                </tr>
                <tr>
                    <td>streetName</td>
                    <td>String</td>
                    <td>The street name of the geolocation</td>
                </tr>
                <tr>
                    <td>streetNumber</td>
                    <td>String</td>
                    <td>The street number of the geolocation</td>
                </tr>
                <tr>
                    <td>postcode</td>
                    <td>String</td>
                    <td>The postcode of the geolocation</td>
                </tr>
                <tr>
                    <td>locality</td>
                    <td>String</td>
                    <td>The locality of the geolocation</td>
                </tr>
                <tr>
                    <td>country</td>
                    <td>String</td>
                    <td>The country of the geolocation</td>
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
                    <td>A given value is null or empty.</td>
                </tr>
                <tr>
                    <td>409</td>
                    <td>Street name, number, locality, and country already exists together.</td>
                </tr>
            </table>
        </td>
    </tr>
    <tr>
        <td>/api/geolocations</td>
        <td>DELETE</td>
        <td>Delete geolocation.</td>
        <td></td>
        <td>
            <table>
                <tr>
                    <th>Status</th>
                    <th>Description</th>
                </tr>
                <tr>
                    <td>400</td>
                    <td>No geolocation matches with the given id.</td>
                </tr>
                <tr>
                    <td>409</td>
                    <td>Supplier or customer is still using this address.</td>
                </tr>
            </table>
        </td>
    </tr>
</table>

---

<a name="customers"></a>
### 4.4 Customers
<table>
    <tr>
        <th>Endpoint</th>
        <th>Method</th>
        <th>Description</th>
        <th>Request Body</th>
        <th>HTTP Status</th>
    </tr>
    <tr>
        <td>/api/customers</td>
        <td>GET</td>
        <td>Get all the customers.</td>
        <td></td>
        <td>
            <table>
                <tr>
                    <th>Status</th>
                    <th>Description</th>
                </tr>
                <tr>
                    <td>204</td>
                    <td>No customer found.</td>
                </tr>
            </table>
        </td>
    </tr>
    <tr>
        <td>/api/customers/{email}</td>
        <td>GET</td>
        <td>Get a customer by his/her email.</td>
        <td></td>
        <td>
            <table>
                <tr>
                    <th>Status</th>
                    <th>Description</th>
                </tr>
                <tr>
                    <td>400</td>
                    <td>No customer matches the given email.</td>
                </tr>
            </table>
        </td>
    </tr>
    <tr>
        <td>/api/customers</td>
        <td>POST</td>
        <td>Create a new customer.</td>
        <td>
            <table>
                <tr>
                    <th>Field</th>
                    <th>Type</th>
                    <th>Description</th>
                </tr>
                <tr>
                    <td>firstName</td>
                    <td>String</td>
                    <td>The first name of the customer</td>
                </tr>
                <tr>
                    <td>lastName</td>
                    <td>String</td>
                    <td>The last name of the customer</td>
                </tr>
                <tr>
                    <td>email</td>
                    <td>String</td>
                    <td>The email of the customer</td>
                </tr>
                <tr>
                    <td>geolocationId</td>
                    <td>Long</td>
                    <td>The id of the geolocation of the customer</td>
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
                    <td>A given value is null or empty. (geolocation can be null)</td>
                </tr>
                <tr>
                    <td>404</td>
                    <td>Geolocation not found.</td>
                </tr>
                <tr>
                    <td>409</td>
                    <td>Email already exists.</td>
                </tr>
            </table>
        </td>
    </tr>
    <tr>
        <td>/api/customers/{email}</td>
        <td>PUT</td>
        <td>Update a customer.</td>
        <td>
            <table>
                <tr>
                    <th>Field</th>
                    <th>Type</th>
                    <th>Description</th>
                </tr>
                <tr>
                    <td>firstName</td>
                    <td>String</td>
                    <td>The first name of the customer</td>
                </tr>
                <tr>
                    <td>lastName</td>
                    <td>String</td>
                    <td>The last name of the customer</td>
                </tr>
                <tr>
                    <td>geolocationId</td>
                    <td>Long</td>
                    <td>The id of the geolocation of the customer</td>
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
                    <td>Customer not found or a given value is null or empty. (geolocation can be null)</td>
                </tr>
                <tr>
                    <td>404</td>
                    <td>Geolocation not found.</td>
                </tr>
            </table>
        </td>
    </tr>
    <tr>
        <td>/api/customers/{email}</td>
        <td>DELETE</td>
        <td>Delete a customer, but not his/her command(s).</td>
        <td></td>
        <td>
            <table>
                <tr>
                    <th>Status</th>
                    <th>Description</th>
                </tr>
                <tr>
                    <td>400</td>
                    <td>Customer not found.</td>
                </tr>
            </table>
        </td>
    </tr>
</table>


#### 4.4.1 Customer orders
<table>
    <tr>
        <th>Endpoint</th>
        <th>Method</th>
        <th>Description</th>
        <th>Request Body</th>
        <th>HTTP Status</th>
    </tr>
    <tr>
        <td>/api/customers/orders</td>
        <td>GET</td>
        <td>Get all the customer orders.</td>
        <td></td>
        <td>
            <table>
                <tr>
                    <th>Status</th>
                    <th>Description</th>
                </tr>
                <tr>
                    <td>204</td>
                    <td>No customer order found.</td>
                </tr>
            </table>
        </td>
    </tr>
    <tr>
        <td>/api/customers/{customerId}/orders</td>
        <td>GET</td>
        <td>Get all the customer orders.</td>
        <td></td>
        <td>
            <table>
                <tr>
                    <th>Status</th>
                    <th>Description</th>
                </tr>
                <tr>
                    <td>204</td>
                    <td>No customer order found.</td>
                </tr>
                <tr>
                    <td>400</td>
                    <td>No customer corresponds to the given id.</td>
                </tr>
            </table>
        </td>
    </tr>
    <tr>
        <td>/api/customers/orders/{id}</td>
        <td>GET</td>
        <td>Get a customer order by its id.</td>
        <td></td>
        <td>
            <table>
                <tr>
                    <th>Status</th>
                    <th>Description</th>
                </tr>
                <tr>
                    <td>400</td>
                    <td>No customer order matches with the given id.</td>
                </tr>
            </table>
        </td>
    </tr>
    <tr>
        <td>/api/customers/orders</td>
        <td>POST</td>
        <td>Create a new customer order.</td>
        <td>
            <table>
                <tr>
                    <th>Field</th>
                    <th>Type</th>
                    <th>Description</th>
                </tr>
                <tr>
                    <td>date</td>
                    <td>LocalDate</td>
                    <td>The date of the order</td>
                </tr>
                <tr>
                    <td>deliveryDate</td>
                    <td>LocalDate</td>
                    <td>The delivery date of the order</td>
                </tr>
                <tr>
                    <td>isSent</td>
                    <td>Boolean</td>
                    <td>True if the order is sent, false otherwise</td>
                </tr>
                <tr>
                    <td>productId</td>
                    <td>Long</td>
                    <td>The id of the product</td>
                </tr>
                <tr>
                    <td>customerId</td>
                    <td>Long</td>
                    <td>The id of the customer</td>
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
                    <td>Order date or delivery date is null.</td>
                </tr>
            </table>
        </td>
    </tr>
    <tr>
        <td>/api/customers/orders/{id}</td>
        <td>PUT</td>
        <td>Update the delivery date of a customer order.</td>
        <td>
            <table>
                <tr>
                    <th>Field</th>
                    <th>Type</th>
                    <th>Description</th>
                </tr>
                <tr>
                    <td>deliveryDate</td>
                    <td>LocalDate</td>
                    <td>The delivery date of the order</td>
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
                    <td>Order not found.</td>
                </tr>
            </table>
        </td>
    </tr>
    <tr>
        <td>/api/customers/orders/{id}/send</td>
        <td>PUT</td>
        <td>Change the state of the customer order as shipped.</td>
        <td></td>
        <td>
            <table>
                <tr>
                    <th>Status</th>
                    <th>Description</th>
                </tr>
                <tr>
                    <td>400</td>
                    <td>No customer order matches with the given id.</td>
                </tr>
                <tr>
                    <td>304</td>
                    <td>A product of the order had a problem.</td>
                </tr>
                <tr>
                    <td>409</td>
                    <td>No customer order is already sent.</td>
                </tr>
            </table>
        </td>
    </tr>
</table>


#### 4.4.2 Customer order lines
<table>
    <tr>
        <th>Endpoint</th>
        <th>Method</th>
        <th>Description</th>
        <th>Request Body</th>
        <th>HTTP Status</th>
    </tr>
    <tr>
        <td>/api/customers/orders/order={orderId}/details</td>
        <td>GET</td>
        <td>Get all the customer order lines of a customer order.</td>
        <td></td>
        <td>
            <table>
                <tr>
                    <th>Status</th>
                    <th>Description</th>
                </tr>
                <tr>
                    <td>204</td>
                    <td>No customer order line found.</td>
                </tr>
                <tr>
                    <td>400</td>
                    <td>No customer order corresponds to the given id.</td>
                </tr>
            </table>
        </td>
    </tr>
    <tr>
        <td>/api/customers/orders/order={orderId}/details/product={productId}</td>
        <td>GET</td>
        <td>Get a customer order line by its id.</td>
        <td></td>
        <td>
            <table>
                <tr>
                    <th>Status</th>
                    <th>Description</th>
                </tr>
                <tr>
                    <td>204</td>
                    <td>No customer order line for this order.</td>
                <tr>
                    <td>400</td>
                    <td>Wrong customer order or product id.</td>
                </tr>
            </table>
        </td>
    </tr>
    <tr>
        <td>/api/customers/orders/order={orderId}/details/product={productId}</td>
        <td>POST</td>
        <td>Create a new customer order line.</td>
        <td>
            <table>
                <tr>
                    <th>Field</th>
                    <th>Type</th>
                    <th>Description</th>
                </tr>
                <tr>
                    <td>quantity</td>
                    <td>Integer</td>
                    <td>The quantity of the product</td>
                </tr>
                <tr>
                    <td>sellPrice</td>
                    <td>Double</td>
                    <td>The selling price of the line.</td>
                </tr>
            </table>
        </td>
        <td>
            <table>
                <tr>
                    <th>Status</th>
                    <th>Description</th>
                <tr>
                    <td>400</td>
                    <td>Wrong customer order or product id.</td>
                </tr>
                <tr>
                    <td>409</td>
                    <td>Order line already exists for this order.</td>
                </tr>
                <tr>
                    <td>412</td>
                    <td>Already sent or delivery date is passed.</td>
                </tr>
                <tr>
                    <td>412</td>
                    <td>Product stock is too low.</td>
                </tr>
                <tr>
                    <td>422</td>
                    <td>Order quantity is too low.</td>
                </tr>
                </tr>
            </table>
        </td>
    </tr>
    <tr>
        <td>/api/customers/orders/order={orderId}/details/product={productId}</td>
        <td>DELETE</td>
        <td>Delete a customer order line.</td>
        <td></td>
        <td>
            <table>
                <tr>
                    <th>Status</th>
                    <th>Description</th>
                </tr>
                <tr>
                    <td>400</td>
                    <td>Wrong customer order or product id.</td>
                </tr>
                <tr>
                    <td>412</td>
                    <td>Already sent or delivery date is passed.</td>
                </tr>
            </table>
        </td>
    </tr>
</table>

---

<a name="suppliers"></a>
### 4.5 Suppliers
<table>
    <tr>
        <th>Endpoint</th>
        <th>Method</th>
        <th>Description</th>
        <th>Request Body</th>
        <th>HTTP Status</th>
    </tr>
    <tr>
        <td>/api/suppliers/</td>
        <td>GET</td>
        <td>Get all the suppliers.</td>
        <td></td>
        <td>
            <table>
                <tr>
                    <th>Status</th>
                    <th>Description</th>
                </tr>
                <tr>
                    <td>204</td>
                    <td>No supplier found.</td>
                </tr>
            </table>
        </td>
    </tr>
    <tr>
        <td>/api/suppliers/{id}</td>
        <td>GET</td>
        <td>Get a supplier by its id.</td>
        <td></td>
        <td>
            <table>
                <tr>
                    <th>Status</th>
                    <th>Description</th>
                </tr>
                <tr>
                    <td>400</td>
                    <td>No supplier found.</td>
                </tr>
            </table>
        </td>
    </tr>
    <tr>
        <td>/api/suppliers</td>
        <td>POST</td>
        <td>Create a new supplier.</td>
        <td>
            <table>
                <tr>
                    <th>Field</th>
                    <th>Type</th>
                    <th>Description</th>
                </tr>
                <tr>
                    <td>name</td>
                    <td>String</td>
                    <td>The name of the supplier.</td>
                </tr>
                <tr>
                    <td>email</td>
                    <td>String</td>
                    <td>The email of the supplier.</td>
                </tr>
                <tr>
                    <td>phoneNumber</td>
                    <td>String</td>
                    <td>The phone number of the supplier.</td>
                </tr>
                <tr>
                    <td>geolocationId</td>
                    <td>Long</td>
                    <td>The id of the geolocation of the supplier.</td>
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
                    <td>Name is null or empty</td>
                </tr>
                <tr>
                    <td>404</td>
                    <td>Geolocation not found.</td>
                </tr>
                <tr>
                    <td>409</td>
                    <td>Supplier already exist by name.</td>
                </tr>
            </table>
        </td>
    </tr>
    <tr>
        <td>/api/suppliers/{id}</td>
        <td>PUT</td>
        <td>Update a supplier. (email or phone number)</td>
        <td>
            <table>
                <tr>
                    <th>Field</th>
                    <th>Type</th>
                    <th>Description</th>
                </tr>
                <tr>
                    <td>name</td>
                    <td>String</td>
                    <td>The name of the supplier.</td>
                </tr>
                <tr>
                    <td>email</td>
                    <td>String</td>
                    <td>The email of the supplier.</td>
                </tr>
                <tr>
                    <td>phoneNumber</td>
                    <td>String</td>
                    <td>The phone number of the supplier.</td>
                </tr>
                <tr>
                    <td>geolocationId</td>
                    <td>Long</td>
                    <td>The id of the geolocation of the supplier.</td>
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
                    <td>Supplier not found ; name is null or empty</td>
                </tr>
                <tr>
                    <td>404</td>
                    <td>Geolocation not found.</td>
                </tr>
                <tr>
                    <td>409</td>
                    <td>Supplier already exist by name.</td>
                </tr>
            </table>
        </td>
    </tr>
    <tr>
        <td>/api/suppliers/{id}</td>
        <td>DELETE</td>
        <td>Delete a supplier.</td>
        <td></td>
        <td>
            <table>
                <tr>
                    <th>Status</th>
                    <th>Description</th>
                </tr>
                <tr>
                    <td>400</td>
                    <td>Supplier not found</td>
                </tr>
                <tr>
                    <td>409</td>
                    <td>Supplier has related orders or products</td>
                </tr>
            </table>
        </td>
    </tr>
</table>

#### 4.5.1 Supplier orders
<table>
    <tr>
        <th>Endpoint</th>
        <th>Method</th>
        <th>Description</th>
        <th>Request Body</th>
        <th>HTTP Status</th>
    </tr>
    <tr>
        <td>/api/suppliers/orders</td>
        <td>GET</td>
        <td>Get all supplier orders.</td>
        <td></td>
        <td>
            <table>
                <tr>
                    <th>Status</th>
                    <th>Description</th>
                </tr>
                <tr>
                    <td>204</td>
                    <td>No supplier orders found.</td>
                </tr>
            </table>
        </td>
    </tr>
    <tr>
        <td>/api/suppliers/{supplierId}/orders</td>
        <td>GET</td>
        <td>Get all supplier orders of a supplier.</td>
        <td></td>
        <td>
            <table>
                <tr>
                    <th>Status</th>
                    <th>Description</th>
                </tr>
                <tr>
                    <td>204</td>
                    <td>No supplier orders found.</td>
                </tr>
                <tr>
                    <td>400</td>
                    <td>No corresponding supplier.</td>
                </tr>
            </table>
        </td>
    </tr>
    <tr>
        <td>/api/suppliers/orders/{id}</td>
        <td>GET</td>
        <td>Get a supplier order.</td>
        <td></td>
        <td>
            <table>
                <tr>
                    <th>Status</th>
                    <th>Description</th>
                </tr>
                <tr>
                    <td>400</td>
                    <td>No corresponding supplier order.</td>
                </tr>
            </table>
        </td>
    </tr>
    <tr>
        <td>/api/suppliers/orders</td>
        <td>POST</td>
        <td>Create a supplier order.</td>
        <td>
            <table>
                <tr>
                    <th>Field</th>
                    <th>Type</th>
                    <th>Description</th>
                </tr>
                <tr>
                    <td>date</td>
                    <td>LocalDate</td>
                    <td>The date of the order.</td>
                </tr>
                <tr>
                    <td>deliveryDate</td>
                    <td>LocalDate</td>
                    <td>The delivery date of the order.</td>
                </tr>
                <tr>
                    <td>orderIsSent</td>
                    <td>Boolean</td>
                    <td>True if the order is sent.</td>
                </tr>
                <tr>
                    <td>isReceived</td>
                    <td>Boolean</td>
                    <td>True if the order is received.</td>
                </tr>
                <tr>
                    <td>supplierId</td>
                    <td>Long</td>
                    <td>The id of the supplier.</td>
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
                    <td>Supplier not found ; date is null ; deliveryDate is null; orderIsSent is null ; isReceived is null</td>
                </tr>
            </table>
        </td>
    </tr>
    <tr>
        <td>/api/suppliers/orders/{id}</td>
        <td>PUT</td>
        <td>Update a supplier order delivery date.</td>
        <td>
            <table>
                <tr>
                    <th>Field</th>
                    <th>Type</th>
                    <th>Description</th>
                </tr>
                <tr>
                    <td>date</td>
                    <td>LocalDate</td>
                    <td>The date of the order.</td>
                </tr>
                <tr>
                    <td>deliveryDate</td>
                    <td>LocalDate</td>
                    <td>The delivery date of the order.</td>
                </tr>
                <tr>
                    <td>orderIsSent</td>
                    <td>Boolean</td>
                    <td>True if the order is sent.</td>
                </tr>
                <tr>
                    <td>isReceived</td>
                    <td>Boolean</td>
                    <td>True if the order is received.</td>
                </tr>
                <tr>
                    <td>supplierId</td>
                    <td>Long</td>
                    <td>The id of the supplier.</td>
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
                    <td>Supplier order not found.</td>
                </tr>
            </table>
        </td>
    </tr>
    <tr>
        <td>/api/suppliers/orders/{id}/send</td>
        <td>PUT</td>
        <td>Update a supplier order to send.</td>
        <td></td>
        <td>
            <table>
                <tr>
                    <th>Status</th>
                    <th>Description</th>
                </tr>
                <tr>
                    <td>400</td>
                    <td>No corresponding supplier order.</td>
                </tr>
                <tr>
                    <td>409</td>
                    <td>Supplier order is already sent or has no line.</td>
                </tr>
            </table>
        </td>
    </tr>
    <tr>
        <td>/api/suppliers/orders/{id}/received</td>
        <td>PUT</td>
        <td>Receive a supplier order.</td>
        <td></td>
        <td>
            <table>
                <tr>
                    <th>Status</th>
                    <th>Description</th>
                </tr>
                <tr>
                    <td>304</td>
                    <td>Product stock of the order had a problem.</td>
                </tr>
                <tr>
                    <td>400</td>
                    <td>No corresponding supplier order.</td>
                </tr>
                <tr>
                    <td>409</td>
                    <td>Supplier order is already received.</td>
                </tr>
            </table>
        </td>
    </tr>
    <tr>
        <td>/api/suppliers/orders/{id}/cancel-reception</td>
        <td>PUT</td>
        <td>Cancel the reception of a supplier order.</td>
        <td></td>
        <td>
            <table>
                <tr>
                    <th>Status</th>
                    <th>Description</th>
                </tr>
                <tr>
                    <td>304</td>
                    <td>Product stock of the order had a problem.</td>
                </tr>
                <tr>
                    <td>400</td>
                    <td>No corresponding supplier order.</td>
                </tr>
                <tr>
                    <td>409</td>
                    <td>Supplier order is not received.</td>
                </tr>
            </table>
        </td>
    </tr>
    <tr>
        <td>/api/suppliers/orders/{id}</td>
        <td>DELETE</td>
        <td>Delete a supplier order.</td>
        <td></td>
        <td>
            <table>
                <tr>
                    <th>Status</th>
                    <th>Description</th>
                </tr>
                <tr>
                    <td>400</td>
                    <td>No corresponding supplier order.</td>
                </tr>
                <tr>
                    <td>412</td>
                    <td>Supplier order is too old.</td>
                </tr>
            </table>
        </td>
    </tr>
    <tr>
        <td>/api/suppliers/orders/{id}/force</td>
        <td>DELETE</td>
        <td>Delete a supplier order by force.</td>
        <td></td>
        <td>
            <table>
                <tr>
                    <th>Status</th>
                    <th>Description</th>
                </tr>
                <tr>
                    <td>400</td>
                    <td>No corresponding supplier order.</td>
                </tr>
            </table>
        </td>
    </tr>
</table>

#### 4.5.2 Supplier order lines
<table>
    <tr>
        <th>Endpoint</th>
        <th>Method</th>
        <th>Description</th>
        <th>Request Body</th>
        <th>HTTP Status</th>
    </tr>
    <tr>
        <td>/api/suppliers/orders/order={orderId}/details</td>
        <td>GET</td>
        <td>Get all supplier order lines of a supplier order.</td>
        <td></td>
        <td>
            <table>
                <tr>
                    <th>Status</th>
                    <th>Description</th>
                </tr>
                <tr>
                    <td>204</td>
                    <td>No supplier order lines found.</td>
                </tr>
                <tr>
                    <td>400</td>
                    <td>No corresponding supplier order.</td>
                </tr>
            </table>
        </td>
    </tr>
    <tr>
        <td>api/suppliers/orders/order={orderId}/details/product={productId}</td>
        <td>GET</td>
        <td>Get a supplier order line of a supplier order.</td>
        <td></td>
        <td>
            <table>
                <tr>
                    <th>Status</th>
                    <th>Description</th>
                </tr>
                <tr>
                    <td>204</td>
                    <td>No supplier order line found.</td>
                </tr>
                <tr>
                    <td>400</td>
                    <td>No corresponding supplier order or product.</td>
                </tr>
            </table>
        </td>
    </tr>
    <tr>
        <td>/api/suppliers/orders/order={orderId}/details/product={productId}</td>
        <td>PUT</td>
        <td>Update a supplier order line of a supplier order.</td>
        <td>
            <table>
                <tr>
                    <th>Field</th>
                    <th>Type</th>
                    <th>Description</th>
                </tr>
                <tr>
                    <td>quantity</td>
                    <td>Integer</td>
                    <td>Quantity of the product.</td>
                </tr>
                <tr>
                    <td>buyPrice</td>
                    <td>Double</td>
                    <td>Buy price of the product.</td>
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
                    <td>Supplier order not found ; product not found.</td>
                </tr>
                <tr>
                    <td>409</td>
                    <td>Supplier order already exists.</td>
                </tr>
                <tr>
                    <td>412</td>
                    <td>Supplier order is already sent ; wrong product supplier ; invalid quantity.</td>
                </tr>
            </table>
        </td>
    </tr>
    <tr>
        <td>/api/suppliers/orders/order={orderId}/details/product={productId}</td>
        <td>DELETE</td>
        <td>Delete a supplier order line of a supplier order.</td>
        <td></td>
        <td>
            <table>
                <tr>
                    <th>Status</th>
                    <th>Description</th>
                </tr>
                <tr>
                    <td>400</td>
                    <td>No corresponding supplier order line.</td>
                </tr>
                <tr>
                    <td>412</td>
                    <td>Supplier order is already sent.</td>
                </tr>
            </table>
        </td>
    </tr>
</table>

---

<a name="brands"></a>
### 4.6 Brands
<table>
    <tr>
        <th>Endpoint</th>
        <th>Method</th>
        <th>Description</th>
        <th>Request Body</th>
        <th>HTTP Status</th>
    </tr>
    <tr>
        <td>/api/brands</td>
        <td>GET</td>
        <td>Get all brands.</td>
        <td></td>
        <td>
            <table>
                <tr>
                    <th>Status</th>
                    <th>Description</th>
                </tr>
                <tr>
                    <td>204</td>
                    <td>No brand found.</td>
                </tr>
            </table>
        </td>
    </tr>
    <tr>
        <td>/api/brands/{id}</td>
        <td>GET</td>
        <td>Get a brand.</td>
        <td></td>
        <td>
            <table>
                <tr>
                    <th>Status</th>
                    <th>Description</th>
                </tr>
                <tr>
                    <td>400</td>
                    <td>No brand found.</td>
                </tr>
            </table>
        </td>
    </tr>
    <tr>
        <td>/api/brands</td>
        <td>POST</td>
        <td>Create a brand.</td>
        <td>
            <table>
                <tr>
                    <th>Field</th>
                    <th>Type</th>
                    <th>Description</th>
                </tr>
                <tr>
                    <td>name</td>
                    <td>String</td>
                    <td>Name of the brand.</td>
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
                    <td>Brand already exists, or the name is null or empty.</td>
                </tr>
                <tr>
                    <td>409</td>
                    <td>Brand name already exists.</td>
                </tr>
            </table>
        </td>
    </tr>
    <tr>
        <td>/api/brands/{id}</td>
        <td>DELETE</td>
        <td>Delete a brand.</td>
        <td></td>
        <td>
            <table>
                <tr>
                    <th>Status</th>
                    <th>Description</th>
                </tr>
                <tr>
                    <td>400</td>
                    <td>Brand not found.</td>
                </tr>
                <tr>
                    <td>409</td>
                    <td>Brand has related products.</td>
                </tr>
            </table>
        </td>
    </tr>
</table>

---

<a name="categories"></a>
### 4.7 Categories
<table>
    <tr>
        <th>Endpoint</th>
        <th>Method</th>
        <th>Description</th>
        <th>Request Body</th>
        <th>HTTP Status</th>
    </tr>
    <tr>
        <td>/api/categories</td>
        <td>GET</td>
        <td>Get all categories.</td>
        <td></td>
        <td>
            <table>
                <tr>
                    <th>Status</th>
                    <th>Description</th>
                </tr>
                <tr>
                    <td>204</td>
                    <td>No category found.</td>
                </tr>
            </table>
        </td>
    </tr>
    <tr>
        <td>/api/categories/{id}</td>
        <td>GET</td>
        <td>Get a category.</td>
        <td></td>
        <td>
            <table>
                <tr>
                    <th>Status</th>
                    <th>Description</th>
                </tr>
                <tr>
                    <td>400</td>
                    <td>No category found.</td>
                </tr>
            </table>
        </td>
    </tr>
    <tr>
        <td>/api/categories</td>
        <td>POST</td>
        <td>Create a category.</td>
        <td>
            <table>
                <tr>
                    <th>Field</th>
                    <th>Type</th>
                    <th>Description</th>
                </tr>
                <tr>
                    <td>name</td>
                    <td>String</td>
                    <td>Name of the category.</td>
                </tr>
                <tr>
                    <td>description</td>
                    <td>String</td>
                    <td>Name of the category.</td>
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
                    <td>Category name is null or empty.</td>
                </tr>
                <tr>
                    <td>409</td>
                    <td>Category name already exists.</td>
                </tr>
            </table>
        </td>
    </tr>
    <tr>
        <td>/api/categories/{id}</td>
        <td>PUT</td>
        <td>Update a category description.</td>
        <td>
            <table>
                <tr>
                    <th>Field</th>
                    <th>Type</th>
                    <th>Description</th>
                </tr>
                <tr>
                    <td>name</td>
                    <td>String</td>
                    <td>Name of the category.</td>
                </tr>
                <tr>
                    <td>description</td>
                    <td>String</td>
                    <td>Name of the category.</td>
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
                    <td>Category not found.</td>
                </tr>
            </table>
        </td>
    </tr>
    <tr>
        <td>/api/categories/{id}</td>
        <td>DELETE</td>
        <td>Delete a category.</td>
        <td></td>
        <td>
            <table>
                <tr>
                    <th>Status</th>
                    <th>Description</th>
                </tr>
                <tr>
                    <td>400</td>
                    <td>Category not found.</td>
                </tr>
                <tr>
                    <td>409</td>
                    <td>Category has related products.</td>
                </tr>
            </table>
        </td>
    </tr>
</table>

---

<a name="products"></a>
### 4.8 Products
<table>
    <tr>
        <th>Endpoint</th>
        <th>Method</th>
        <th>Description</th>
        <th>Request Body</th>
        <th>HTTP Status</th>
    </tr>
    <tr>
        <td>/api/products</td>
        <td>GET</td>
        <td>Get all products.</td>
        <td></td>
        <td>
            <table>
                <tr>
                    <th>Status</th>
                    <th>Description</th>
                </tr>
                <tr>
                    <td>204</td>
                    <td>No product found.</td>
                </tr>
            </table>
        </td>
    </tr>
    <tr>
        <td>/api/products/{id}</td>
        <td>GET</td>
        <td>Get a product.</td>
        <td></td>
        <td>
            <table>
                <tr>
                    <th>Status</th>
                    <th>Description</th>
                </tr>
                <tr>
                    <td>400</td>
                    <td>No product found.</td>
                </tr>
            </table>
        </td>
    </tr>
    <tr>
        <td>/api/products/{id}/details</td>
        <td>GET</td>
        <td>Get product details.</td>
        <td></td>
        <td>
            <table>
                <tr>
                    <th>Status</th>
                    <th>Description</th>
                </tr>
                <tr>
                    <td>400</td>
                    <td>No product found.</td>
                </tr>
            </table>
        </td>
    </tr>
    <tr>
        <td>/api/products/low</td>
        <td>GET</td>
        <td>Get all products with low stock. </td>
        <td></td>
        <td>
            <table>
                <tr>
                    <th>Status</th>
                    <th>Description</th>
                </tr>
                <tr>
                    <td>204</td>
                    <td>No product found.</td>
                </tr>
            </table>
        </td>
    </tr>
    <tr>
        <td>/api/products</td>
        <td>POST</td>
        <td>Create a product.</td>
        <td>
            <table>
                <tr>
                    <th>Field</th>
                    <th>Type</th>
                    <th>Description</th>
                </tr>
                <tr>
                    <td>name</td>
                    <td>String</td>
                    <td>Name of the product.</td>
                </tr>
                <tr>
                    <td>description</td>
                    <td>String</td>
                    <td>Description of the product.</td>
                </tr>
                <tr>
                    <td>purchasePrice</td>
                    <td>Double</td>
                    <td>Purchase price of the product.</td>
                </tr>
                <tr>
                    <td>salePrice</td>
                    <td>Double</td>
                    <td>Sale price of the product.</td>
                </tr>
                <tr>
                    <td>stock</td>
                    <td>Integer</td>
                    <td>Stock of the product.</td>
                </tr>
                <tr>
                    <td>minStock</td>
                    <td>Integer</td>
                    <td>Minimum stock of the product.</td>
                </tr>
                <tr>
                    <td>batchSize</td>
                    <td>Integer</td>
                    <td>Batch size of the product.</td>
                </tr>
                <tr>
                    <td>brandId</td>
                    <td>Long</td>
                    <td>Brand id of the product.</td>
                </tr>
                <tr>
                    <td>categoryId</td>
                    <td>Long</td>
                    <td>Category id of the product.</td>
                </tr>
                <tr>
                    <td>supplierId</td>
                    <td>Long</td>
                    <td>Supplier id of the product.</td>
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
                    <td>Invalid product information.</td>
                </tr>
                <tr>
                    <td>404</td>
                    <td>Category, brand, or supplier not found.</td>
                </tr>
                <tr>
                    <td>409</td>
                    <td>Product already exists by name and supplier id.</td>
                </tr>
            </table>
        </td>
    </tr>
    <tr>
        <td>/api/products/{id}</td>
        <td>PUT</td>
        <td>Update a product.</td>
        <td>
            <table>
                <tr>
                    <th>Field</th>
                    <th>Type</th>
                    <th>Description</th>
                </tr>
                <tr>
                    <td>name</td>
                    <td>String</td>
                    <td>Name of the product.</td>
                </tr>
                <tr>
                    <td>description</td>
                    <td>String</td>
                    <td>Description of the product.</td>
                </tr>
                <tr>
                    <td>purchasePrice</td>
                    <td>Double</td>
                    <td>Purchase price of the product.</td>
                </tr>
                <tr>
                    <td>salePrice</td>
                    <td>Double</td>
                    <td>Sale price of the product.</td>
                </tr>
                <tr>
                    <td>stock</td>
                    <td>Integer</td>
                    <td>Stock of the product.</td>
                </tr>
                <tr>
                    <td>minStock</td>
                    <td>Integer</td>
                    <td>Minimum stock of the product.</td>
                </tr>
                <tr>
                    <td>batchSize</td>
                    <td>Integer</td>
                    <td>Batch size of the product.</td>
                </tr>
                <tr>
                    <td>brandId</td>
                    <td>Long</td>
                    <td>Brand id of the product.</td>
                </tr>
                <tr>
                    <td>categoryId</td>
                    <td>Long</td>
                    <td>Category id of the product.</td>
                </tr>
                <tr>
                    <td>supplierId</td>
                    <td>Long</td>
                    <td>Supplier id of the product.</td>
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
                    <td>Invalid product information.</td>
                </tr>
                <tr>
                    <td>404</td>
                    <td>Category, brand, or supplier not found.</td>
                </tr>
            </table>
        </td>
    </tr>
    <tr>
        <td>/api/products/{id}</td>
        <td>DELETE</td>
        <td>Delete a product.</td>
        <td></td>
        <td>
            <table>
                <tr>
                    <th>Status</th>
                    <th>Description</th>
                </tr>
                <tr>
                    <td>400</td>
                    <td>Product not found.</td>
                </tr>
                <tr>
                    <td>409</td>
                    <td>Product has relationships.</td>
                </tr>
            </table>
        </td>
    </tr>
</table>
