# Data Layer docs

This is a guide to the data layer in the app and the components that it is built of

## Design/Organization requirements

Most importantly, all data operations must live in Business, not the app layer. The app layer should make calls to models to retrieve and send data.

The system is split up into several components.

### Models
A model is just a plain Kotlin data class that holds the info for one thing. It has no database code in it. It practically never knows firestore exists.

- Views, Viewmodels, and repositories all pass models around
- A model can contain simple logic about itself

### Field Object
This is an object of constants naming the collection and its fields. Every read/write operation uses these instead of strings to avoid typos, which in this case, would cause a compile error instead.

### Mapper
A mapper converts a stored doc into a model.

### Operations
An operation is a small class that does exactly one database job. Pass in params in the constructor. 

an operation can be ran using `service.execute(operation)` or `service.stream(...)`

### Query
A query only reads data, never changes it. It should return a model, list of models, or a page.

Use a query when you only need to look at data

### Command
A command is a single write; create, update, or delete one thing. It should either return the new ID or nothing.

Use a command when one write is enough and it doesn't depend on what is currently stored.

### Transaction
A transaction reads something and then writes based on what it read. For example, reading the number of upvotes on a post, then increasing by one.

Use a transaction when what you write depends on what you read and two things must stay consistent

### Batch
A batch is a group of writes that all succeed or all fail together. A batch cannot read.

Use a batch when you already know every write and they must land together.

### Stream
A stream keeps sending new values whenever the data changes, instead of answering once. Collecting it starts a listener and cancelling stops it.

- Each update says whether it came from the local cache or the server
- use these sparingly. They are expensive.

Use a stream when the screen must update live without the user doing anything

### Paged Query
A query varient that returns one page and a cursor for the next. A pager collects pages into a growing list and loads the next page when asked.

### Repository
A repository is the single entry point for the UI. It decides which operations to run, combines their results, keeps the current list in memory, and exposes simple functions



## Backend Components

FirestoreService - runs any operation. It times it, reports it to diagnostics, applies the optional timeout, and converts every failure into a FirestoreException with a kind. It contains no feed logic

FirestoreContext - this is what an operation receives. The firestore connection plus safe helpers for building paths and reading with a required limit.

AppContainer - this is where the service and repositories are created, once, when the app stats. The application class holds it and the ViewModel factories ask it for repositories.

## Errors
PERMISSION_DENIED - security rules rejected the request
UNAVAILABLE - cannot reach firestore
NOT_FOUND - updating something that doesnt exist
INVALID_ARGUMENT - bad input
SCHEMA_MISMATCH - a stored document doesnt match its mapper
