# Firebase Engine V2
* V2 design for the core firebase engine

## Summary
The idea here is to create an engine/core service that allows a developer to create declaritive files for database operations. The files are essentially SQL files with a more limited syntax. These files are then parsed and passed through a series of checks before being converted into a model the firebase database can accept. Other pages within the system can interact with the database through public interfaces that streamline this procses.

## Changes over V1
- procedures are compiled, checked, and built into planned execution plans during the build of the app. Errors/warnings regarding the procs are identified and in some cases, cancel the build.

- Valid planned execution plans should be cached into the app during the build. This way, during runtime, the app is lot spending resources to re-compile and type check a proc when it is called.

- Moving towards and storing planned execution plans also opens us up for a space to do optimizations and reduce calls to avoid the daily quota of the firebase free tier, should that become an issue

## Pipeline
### On Build Init

1. A dev creates a .fsql file and defines a procedure using T-SQL

2. The .fsql file undergoes checks such as parameter type validation and detection of unsupported keywords.

3. The .fsql file, which should be valid SQL, is passed into a third party SQL parser to check that its syntax is valid.

4. Once all checks are passed, the SQL is converted into a firebase compitable planned execution plan.

5. Planned Execution plans are then built into the app (possible .plan ext)

### End Build

### Runtime

1. A page requests a database call through the internal API

2. Planned execution plan for the desired proc(s) are loaded in from local files

3. Plans are sent over to the core service along with any required parameters, supplied in the initial api call

4. Plans are put into a queue in the order they are received

5. If we choose to optimize queries, it would occur at this stage via modifications to planned execution plans. These tweaks can include combining multiple plans together.

6. The service processes the plan, posts to the database, and waits for a response (a error is thrown if no response is given after a period of time. Aka, request timeout).

7. This response is then loaded into a formatted object and returned to the original function caller.

### End runtime execution

## Internal API
The internal api for the system is relatively simple. It is composed of 3 main objects; DbQuery, DbError, and DbResult.

### DbQuery
* used to create a request to the database.

sudo-code:
val query = DbQuery("getUserId", username, password)

val result = query.execute()

### DbResult
* This is the result returned from the database after a call. Contains the following data:

- values
- errors
- rows affected

Values - any data that gets returned from the call

errors - a list of any errors encountered while attempting to execute the proc

rows affected - the number of rows affected by the call to the db

### DbError
* contains all error information for an error encountered during the execution process

These are returned and not thrown in most cases. This way, the app does not crash if, for example, a call times out.

## Sources of complexity

- build time integration for compile checks of .fsql files

- execution plan optimizations (TBD)

- Advanced T-SQL syntax support (optional)