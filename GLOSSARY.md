# Worker Housing

MyWorkerLodger is an operations tool for one housing operator to manage accommodation where workers are housed: who sleeps where, how full each place is, and who is moving in or out.

## Language

### People

**Accommodation Manager**:
The person who runs worker accommodation day to day and the only kind of person who uses the app.
_Avoid_: Admin, landlord, operator, user

**Worker**:
A person who can be housed in the accommodation; they remain a Worker before, during and after living there.
_Avoid_: Lodger, tenant, resident, occupant

**Employer**:
The company a Worker works for, which sends its Workers to be housed; each Worker has exactly one Employer at a time.
_Avoid_: Client, company, sponsor

**Staff Number**:
The identifier an Employer gives a Worker; together with the Employer it identifies the Worker uniquely.
_Avoid_: Employee ID, worker ID

### Space

**Property**:
A building or site used to house Workers.
_Avoid_: Camp, building, site, accommodation

**Room**:
A space within a Property that contains one or more Beds and is designated either male or female.
_Avoid_: Unit, dorm

**Bed**:
A single sleeping place within a Room, and the unit a Worker is assigned to.
_Avoid_: Bunk, berth, slot, space

**Out of Service**:
The state of a Bed that cannot be used, for example because it is broken or under repair; no Worker can check in to it.
_Avoid_: Disabled, blocked, closed

### Living there

**Stay**:
One Worker living in one Bed, from Check-in until Check-out; a Stay with no Check-out yet is the Worker's current Stay.
_Avoid_: Allocation, assignment, booking, occupancy, tenancy

**Check-in**:
The moment a Worker actually arrives and a Stay begins.
_Avoid_: Move-in, arrival, allocation

**Check-out**:
The moment a Worker leaves a Bed and their Stay ends.
_Avoid_: Move-out, departure, release

**Move**:
Taking a Worker from one Bed to another on the same date: their current Stay ends with a Check-out and a new Stay begins with a Check-in.
_Avoid_: Transfer, relocation, reassignment

### Measures

**Occupancy**:
The share of in-service Beds that have a current Stay, for a Room, a Property or overall.
_Avoid_: Utilisation, fill rate, capacity

**Available Bed**:
An in-service Bed with no current Stay.
_Avoid_: Free bed, vacancy, empty bed
