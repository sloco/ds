package ni.edu.uccleon

import grails.validation.ValidationException
import grails.util.Environment

class UserController {

    UserService userService

    static defaultAction = 'profile'

    static allowedMethods = [
        list: ['GET', 'POST'],
        create: ['GET', 'POST'],
        show: ['GET', 'POST'],
        notification: 'POST',
        updatePassword: 'POST',
        profile: ['GET', 'POST'],
        classrooms: ['GET', 'POST'],
    ]

    def admin() {
        redirect action: "coordsAndRooms"
    }

    def coordsAndRoomsFlow = {
        init {
            action {
                flow.userCoordinations = session?.user?.refresh()?.schools as List
                flow.users =  User.where {
                    enabled == true && role in ['user', 'coordinador']
                }.list(sort: 'fullName', order: 'asc')

                if (flow.userCoordinations.size() > 1) {
                    coordinations()
                } else {
                    flow.coordination = flow.userCoordinations[0]
                    roster()
                }
            }

            on("coordinations").to "coordinations"
            on("roster").to "roster"
        }

        coordinations {
            on("confirm") {
                flow.coordination = params?.coordination
                if (flow.users) {
                    flow.users.collect { it.refresh() }
                }

                if (!flow.coordination) {
                    flash.message = "Selecciona una coordinacion"
                    return error()
                }
            }.to "roster"
        }

        roster {
            on("back").to "coordinations"
            on("classrooms") {
                def user = User.get params?.id

                if (!user) {
                    response.sendError 404
                }

                if (flow.users) {
                    flow.users.collect { it.refresh() }
                }

                [
                    user: user,
                    classrooms: userService.getClassrooms(user.email),
                    userClassrooms: user.classrooms
                ]
            }.to "classrooms"
        }

        classrooms {
            on("back").to "roster"
        }
    }

    def addingOrRemovingUserCoordinationsOrClassrooms(Integer id, String data, String flag, Boolean state) {
        User user = User.get id

        if (state) {
            if (flag == "classrooms") {
                user.addToClassrooms data
            } else {
                user.addToSchools data
            }
        } else {
            if (flag == "classrooms") {
                user.removeFromClassrooms data
            } else {
                user.removeFromSchools data
            }
        }

        if (!user.save(flush: true)) {
            user.errors.allErrors.each { error ->
                log.error "[$error.field: $error.defaultMessage]"
            }
        }

        render(contentType: "application/json") {
            error = user.hasErrors()
            email = user.email
            fullName = user.fullName
        }
    }

    def list() {
        final String query = """
            SELECT
                new map (u.id AS id, u.fullName AS fullName)
            FROM
                User u
            WHERE
                u.enabled = true
            ORDER BY u.fullName"""

        [users: User.executeQuery(query)]
    }

    def filter() {}

    def applyFilter() {
        List<Boolean> enabled = params.list('enabled')*.toBoolean()
        List<String> departments = params.list('departments')
        List<String> schools = params.list('schools')
        List<String> roles = params.list('roles')
        List<User> users = []

        List<User> userList = User.createCriteria().list {
            if (enabled) {
                'in' 'enabled', enabled
            }

            if (roles) {
                'in' 'role', roles
            }

            order 'fullName', 'asc'
        }

        if (schools || departments) {
            users = userList.findAll { User user ->
                List<String> schoolList = userService.getUserSchools(user.id)

                schoolList.any { school ->
                    school in schools || school in departments
                }
            }
        }

        render view: 'list', model: [users: users ?: userList]
    }

    def create() {
        [userModel: makeUserModel()]
    }

    def save(SaveUser command) {
        if (command.hasErrors()) {
            render model: [errors: command.errors, userModel: makeUserModel()], view: 'create'

            return
        }

        try {
            User user = userService.save(command)

            if (Environment.current == Environment.PRODUCTION) {
                List<User> authorities = getAuthoritiesInSchools(command.schools)

                sendNotification(authorities, user)
            }

            flash.message = Environment.current == Environment.PRODUCTION ? 'Usuario creado y notificado' : 'Usuario creado'
            redirect action: 'create', method: 'GET'
        } catch(ValidationException e) {
            render model: [errors: e.errors, userModel: makeUserModel()], view: 'create'
        }
    }

    def show(Integer id) {
        Map user = userService.getUserDataset(id)

        if (!user.fullName) response.sendError 404

        [user: user]
    }

    def edit(final Long id) {
        Map user = userService.getUserDataset(id)

        if (!user.fullName) response.sendError 404

        [user: user, userModel: makeUserModel()]
    }

    def update(UpdateUser command) {
        if (command.hasErrors()) {
            render model: [
                user: userService.getUserDataset(command.id),
                userModel: makeUserModel(),
                errors: command.errors
            ], view: 'edit'

            return
        }

        try {
            User user = userService.update(command)
            flash.message = 'Datos actualizados'

            redirect action: 'edit', id: command.id
        }
        catch(ValidationException e) {
            render model: [user: userService.getUserDataset(command.id), userModel: makeUserModel()], view: 'edit'
        }
    }

    def updateUserEnabledProperty(Integer id) {
        User user = User.get(id)

        user.properties["enabled"] = !user.enabled

        String message = user.save() ? 'success' : 'error'

        render(contentType: "application/json") {
            status = message
        }
    }

    def notification(Integer id) {
        def user = User.get id

        if (!user) response.sendError 404

        sendMail {
            to user.email
            subject 'Sobre solicitudes de datashow'
            html g.render(template: 'email', model : [user: user, host: getServerURL()])
        }

        flash.message = 'Notificacion enviada'
        redirect action: 'show', id: id
    }

    def delete(Integer id) {
        def user = User.get(id)

        if (!user) {
            response.sendError 404
        }

        user.delete()

        flash.message = "Usuario eliminado"
        redirect action:"list"
    }

    def profile() {
        User user = session.user

        if (request.post) {
            User.executeUpdate('''
                UPDATE
                    User
                SET
                    fullName = :fullName
                WHERE
                    id = :id''',
                [fullName: params.fullName, id: user.id])

            flash.message = 'Perfil actualizado. Reiniciar sesion para verificar cambio'
        }

        [user: user, schools: userService.getCurrentUserSchools()]
    }

    def classrooms() {
        User user = session?.user

        if (request.method == 'POST') {
            List<String> classrooms = params.list('classrooms')

            if (classrooms) {
                userService.addClassrooms(classrooms, user)
            }
        }

        [
            user: user,
            allCls: userService.getClassrooms(user.email),
            classrooms: userService.getUserClassrooms(user.id),
        ]
    }

    def password() {}

    def updatePassword(UpdatePasswordCommand cmd) {
        if (cmd.hasErrors()) {
            render view: 'password', model: [errors: cmd.errors]

            return
        }

        User.executeUpdate('''
            UPDATE
                User
            SET
                password = :password
            WHERE
                id = :id''', [password: cmd.npassword.encodeAsSHA1(), id: cmd.id])

        flash.message = 'Clave actualizada'
        redirect action: 'password', params: [id: cmd.id]
    }

    def resetPassword(Integer id) {
        User user = User.get(id)

        if (!user) response.sendError 404

        user.with {
            password = '1234567'
            save(flush: true)
        }

        flash.message = 'Clave resumida'
        redirect action:'show', id: id
    }

    private String getServerURL() {
        grailsApplication.config.grails.serverURL
    }

    private List<User> getAuthoritiesInSchools(List<String> schoolList) {
        List<User> authorities = getAcademicAuthorities().inject([]) { accumulator, currentValue ->
            if (currentValue.schools.toList().any { school -> school in schoolList }) {
                accumulator << currentValue
            }

            accumulator
        }

        authorities
    }

    private List<User> getAcademicAuthorities() {
        User.hasAcademyAuthority.list()
    }

    private void sendNotification(final List<User> authorities, final User user) {
        authorities.each { authority ->
            sendMail {
                to authority.email
                subject 'Notificacion de datashow'
                html g.render (template: 'email', model: [
                    authority: authority.fullName,
                    fullName: user.fullName,
                    schools: userService.getUserSchools(user.id),
                    classrooms: userService.getUserClassrooms(user.id)
                ])
            }
        }
    }

    private UserModel makeUserModel() {
        new UserModel (
            roles: grailsApplication.config.ni.edu.uccleon.roles,
            classrooms: grailsApplication.config.ni.edu.uccleon.cls,
            schoolsAndDepartments: grailsApplication.config.ni.edu.uccleon.schoolsAndDepartments
        )
    }
}

class UserModel {
    List<String> roles
    Map classrooms
    Map schoolsAndDepartments
}

class UpdatePasswordCommand {
    Long id
    String password
    String npassword
    String rpassword

    static constraints = {
        password blank:false
        npassword blank:false
        rpassword blank:false, validator:{rpassword, obj -> rpassword == obj.npassword }
    }
}

class SaveUser {
    String fullName
    String email
    String role
    List<String> schools
    List<String> classrooms

    static constraints = {
        fullName blank: false
        email blank: false, unique: true, email: true
        role blank: false
        schools nullable: false, min: 1
        classrooms nullable: false, min: 1
    }
}

class UpdateUser extends SaveUser {
    Long id
}
