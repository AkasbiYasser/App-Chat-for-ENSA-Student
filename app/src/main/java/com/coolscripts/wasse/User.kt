package com.coolscripts.wasse

class User {
    var prenom: String? = null
    var nom: String? = null
    var numApogee: String? = null
    var email: String? = null
    var uid: String? = null
    var filiere: String? = null
    var lastMessageTime: Long? = null

    constructor() {}

    constructor(prenom: String?, nom: String?, numApogee: String?, email: String?, uid: String?, filiere: String?, lastMessageTime: Long?) {
        this.prenom = prenom
        this.nom = nom
        this.numApogee = numApogee
        this.email = email
        this.uid = uid
        this.filiere = filiere
        this.lastMessageTime = lastMessageTime
    }
}
