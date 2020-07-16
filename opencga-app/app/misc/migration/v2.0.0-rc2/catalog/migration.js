// Remove null values of jobId
db.file.update({"jobId": null}, {"$set": {"jobId": ""}});
db.job.find({}, {id:1, output:1, stdout:1, stderr:1}).forEach(function(job) {
    fileUids = [];
    if (isNotEmptyArray(job.output)) {
        for (file of job.output) {
            fileUids.push(file.uid);
        }
    }
    if (isNotUndefinedOrNull(job.stdout) && isNotUndefinedOrNull(job.stdout.uid)) {
        fileUids.push(job.stdout.uid);
    }
    if (isNotUndefinedOrNull(job.stderr) && isNotUndefinedOrNull(job.stderr.uid)) {
        fileUids.push(job.stderr.uid);
    }

    // Update file documents
    print(job.id + ": " + fileUids);
    db.file.update({"uid": {"$in": fileUids}}, {"$set": {"jobId": "caca"}}, {"multi": true});
});

// Fix clinical issue where member ids were not populated
function _getMemberId(memberUidToIdMap, memberUid) {
    if (memberUid in memberUidToIdMap) {
        return memberUidToIdMap[memberUid];
    } else {
        // If it is not in the map, we find it and store in map
        var memberId = db.individual.findOne({uid: memberUid}, {id: 1})['id'];
        memberUidToIdMap[memberUid] = memberId;
        return memberId;
    }
}

migrateCollection("clinical", {}, {}, function(bulk, doc) {
    var memberUidToIdMap = {};
    if (isNotUndefinedOrNull(doc.family)) {
        if (isNotEmptyArray(doc.family.members)) {
            doc.family.members.forEach(function(member) {
                memberUidToIdMap[member['uid']] = member['id'];
            });

            doc.family.members.forEach(function(member) {
                if (isNotUndefinedOrNull(member.father) && isNotUndefinedOrNull(member.father.uid)) {
                    member['father']['id'] = _getMemberId(memberUidToIdMap, member['father']['uid']);
                }
                if (isNotUndefinedOrNull(member.mother) && isNotUndefinedOrNull(member.mother.uid)) {
                    member['mother']['id'] = _getMemberId(memberUidToIdMap, member['mother']['uid']);
                }
            });
        }
    }
    if (isNotUndefinedOrNull(doc.proband)) {
        if (isNotUndefinedOrNull(doc.proband.father) && isNotUndefinedOrNull(doc.proband.father.uid)) {
            doc['proband']['father']['id'] = _getMemberId(memberUidToIdMap, doc['proband']['father']['uid']);
        }
        if (isNotUndefinedOrNull(doc.proband.mother) && isNotUndefinedOrNull(doc.proband.mother.uid)) {
            doc['proband']['mother']['id'] = _getMemberId(memberUidToIdMap, doc['proband']['mother']['uid']);
        }
    }

    bulk.find({"_id": doc._id}).updateOne({"$set": {
            'proband': doc.proband,
            'family': doc.family
        }
    });
});

// Set all _individualUid to NumberLong(-1) from sample collection
db.sample.update({_individualUid:-1},{$set:{_individualUid:NumberLong(-1)}},{multi:true})


// Remove proband and family references in clinical analysis #1625
function _getSampleIdReferences(sample) {
    if (isUndefinedOrNull(sample)) {
        return sample;
    }
    return {
        'uid': sample['uid'],
        'id': sample['id'],
        'version': sample['version']
    }
}

function _getMemberAndSampleIdReferences(member) {
    if (isUndefinedOrNull(member)) {
        return member;
    }
    var newMember = {
        'uid': member['uid'],
        'id': member['id'],
        'version': member['version'],
        'samples': member['samples']
    };

    if (isNotEmptyArray(member.samples)) {
        var samples = [];
        for (var sample of member.samples) {
            samples.push(_getSampleIdReferences(sample));
        }
        newMember['samples'] = samples;
    }

    return newMember;
}

migrateCollection("clinical", {}, {proband: 1, family: 1}, function(bulk, doc) {
    var toset = {};

    if (isNotUndefinedOrNull(doc.family)) {
        var family = {
            'uid': doc.family['uid'],
            'id': doc.family['id'],
            'version': doc.family['version'],
            'members': doc.family['members']
        };
        if (isNotEmptyArray(doc.family.members)) {
            var members = [];
            for (var member of doc.family.members) {
                members.push(_getMemberAndSampleIdReferences(member));
            }
            family['members'] = members;
        }

        toset['family'] = family;
    }

    if (isNotUndefinedOrNull(doc.proband)) {
        toset['proband'] = _getMemberAndSampleIdReferences(doc.proband);
    }

    bulk.find({"_id": doc._id}).updateOne({"$set": toset});
});