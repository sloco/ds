package ni.edu.uccleon

class UserController {

    def userService

	static defaultAction = "login"
    static allowedMethods = [
    	list:["GET", "POST"],
    	create:["GET", "POST"],
    	show:["GET", "POST"],
    	delete:"GET",
    	login:["GET", "POST"],
    	register:["GET", "POST"],
    	updatePassword:"POST",
    	resetPassword:"GET",
        profile:["GET", "POST"]
    ]

    def list() {
    	def users

    	if (!params.confirmed) {
    		users = User.listByRole("user").list()
    	} else {
    		users = User.listByRole("user").findAllByEnabled(params.confirmed)
    	}

    	if (request.post) {
    		users = User.listByRole("user").search(params.q).list()
    	}

        [users:users]
    }

    def create() {
        if (request.get) {
            [user:new User(params)]
        } else {
            def user = new User(params)

            if (!user.save()) {
                return [user:user]
            }

            flash.message = "data.saved"
        }
    }

    def show(Integer id) {
        def user = User.get(id)

        if (!user) {
            response.sendError 404
            return false
        }

        if (request.get) {
            return [user:user]
        } else if (request.post) {
            user.properties = params

            if (!user.save()) {
                return [user:user]
            }

            flash.message = "data.saved"
        }
    }

    def delete(Integer id) {
        def user = User.get(id)

        if (!user) {
            response.sendError 404
            return false
        }

        user.delete()

        flash.message = "data.deleted"
        redirect action:"list"
    }

    def profile() {
        def user = User.findByEmail(session?.user?.email)

        if (request.post) {
            user.properties["email", "fullName"] = params
            //update session.user email property
            session?.user?.email = params.email

            if (!user.save()) {
                return [user:user]
            }

            userService.addSchoolsAndUserClassrooms(params, user)
        }

        [user:user]
    }

    def password() { }

    def updatePassword(updatePasswordCommand cmd) {
        if (!cmd.validate()) {
            chain action:(params.path) ?: "password", model:[cmd:cmd], params:[id:cmd.id]
            return
        }

        def user = User.get(cmd.id)

        user.properties["password"] = cmd.npassword
        user.save()

        flash.message = "dato.guardado"
        redirect action:(params.path) ?: "password", params:[id:cmd.id]
    }


    def login(String email, String password) {
        if (request.post) {
            def user = User.login(email, password).get()

            if (!user) {
                flash.message = "user.not.found"
            } else {
            	session.user = user
                redirect controller:"request"
                return false
            }
        }
    }

    def register(userRegisterCommand cmd) {
        if (request.post) {

            if (!cmd.validate()) {
                return [cmd:cmd]
            }

            def user = cmd.createUser()
            user.save()

            //TODO:create confirmation email
            //*/

            //render params
        }
    }

    def resetPassword(Integer id) {
    	def user = User.get(id)

    	if (!user) {
    		response.sendError 404
    	}

    	//TODO:generate token of 7 values
    	user.properties["password"] = "1234567"

    	if (!user.save()) {
    		flash.message = "something.when.wrong"
    		redirect action:"show", params:[id:id]
    		return false
    	}

    	flash.message = "dato.guardado"
    	redirect action:"show", params:[id:id]
    }

    def logout() {
        session.user = null
        redirect action:"login"
    }

}

class userRegisterCommand {
    String email
    String password
    String rpassword
    String fullName

    static constraints = {
        importFrom User
        rpassword blank:false, validator:{val, obj ->
            return val == obj.password
        }
    }

    User createUser() {
        def user = new User(email:email, password:password, fullName:fullName)
        //add schools to user
        /*
        schools.each {school ->
            user.addToSchools(name:school)
        }
        */
        user
    }

}

class updatePasswordCommand {
	Integer id
	String password
	String npassword
	String rpassword

	static constraints = {
		password blank:false
		npassword blank:false
		rpassword blank:false, validator:{rpassword, obj ->
			return rpassword == obj.npassword
		}
	}

}