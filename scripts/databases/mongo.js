db.getSiblingDB("imazsak");

// from tutelar
// db.createCollection("users");
// db.users.createIndex({"id": 1}, {"unique": true});
// db.users.createIndex({"accounts.authType": 1, "accounts.externalId": 1}, {"unique": true});

db.createCollection("groups");
db.groups.createIndex({"id": 1}, {"unique": true});
db.groups.createIndex({"members.id": 1});
