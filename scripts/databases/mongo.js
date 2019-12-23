db.getSiblingDB("imazsak");

// from tutelar
db.createCollection("users");
db.users.createIndex({"id": 1}, {"unique": true});
db.users.createIndex({"accounts.authType": 1, "accounts.externalId": 1}, {"unique": true});
db.users.createIndex({"push.deviceId": 1}, {"unique": true});

db.createCollection("groups");
db.groups.createIndex({"id": 1}, {"unique": true});
db.groups.createIndex({"members.id": 1});

db.createCollection("prayers");
db.prayers.createIndex({"id": 1}, {"unique": true});
db.prayers.createIndex({"userId": 1});
db.prayers.createIndex({"groupIds": 1});

db.createCollection("notifications");
db.notifications.createIndex({"id": 1}, {"unique": true});
db.notifications.createIndex({"userId": 1});

db.createCollection("feedback");
db.feedback.createIndex({"id": 1}, {"unique": true});

db.createCollection("tokens");
db.tokens.createIndex({"tokenType": 1, "token": 1}, {"unique": true});

db.createCollection("stats");
db.stats.createIndex({"id": 1}, {"unique": true});
