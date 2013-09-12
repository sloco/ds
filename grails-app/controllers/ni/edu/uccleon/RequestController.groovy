package ni.edu.uccleon

class RequestController {

    def requestService
    def beforeInterceptor = [action: this.&checkRequestStatus, only: ["editRequestFlow" ,"delete"]]

    static defaultAction = "list"
    static allowedMethods = [
        list:["GET", "POST"],
        create:["GET", "POST"],
        edit:"GET",
        show:"GET",
        updte:"POST",
        delete:["GET"],
        updateStatus:"GET",
        requestsBySchools:["GET", "POST"],
        requestsByClassrooms:["GET", "POST"],
        requestsByUsers:["GET", "POST"],
        disponability:"POST"
    ]

    private checkRequestStatus() {
        def req = Request.get(params?.id)

        if (!req) {
            response.sendError 404
        }

        if (req.status != "pending") {
            response.sendError 403
        }
    }

    def list() {
    	def requests
        def user = session?.user
        def role = user?.role

        if (params.requestFromDate && params.requestToDate) {
            def today = new Date().format("yyyy-MM-dd")

            requests = Request.requestFromTo((params?.requestFromDate) ?: today , (params?.requestToDate) ?: today).list()
        } else {
            if (role == "admin") {
                //TODO:display today requests
                requests = Request.todayRequest().list()
                //requests = Request.list(params)
            } else {
                //TODO:requests must be sorted by dateOfApplication
                requests = Request.listByUser(user).list(params)
            }
        }

    	[requests:requests]
    }

    def createRequestFlow = {
        getRequestType {
            action {
                flow.type = (params.type) ?: "common"
            }

            on("success").to "buildRequest"
        }

        buildRequest {
            on("create") {
                //check for screens, speakers, internet availablity
                def screensAvailable = grailsApplication.config.ni.edu.uccleon.screens
                def speakersAvailable = grailsApplication.config.ni.edu.uccleon.speakers

                def screensResult = Request.countByDateOfApplicationAndScreen(parseDate(params?.dateOfApplication), true)
                if (params.screen && screensResult == screensAvailable) {
                    flash.message = "Todas las pantallas estan ya solicitadas para esta fecha"
                    return error()
                }

                def speakersResult = Request.countByDateOfApplicationAndAudio(parseDate(params?.dateOfApplication), true)
                if (params.audio && speakersResult == speakersAvailable) {
                    flash.message = "Todas las parlantes estan ya solicitadas para esta fecha"
                    return error()
                }

                def req = new Request(
                    dateOfApplication:parseDate(params?.dateOfApplication),
                    classroom:params?.classroom,
                    school:params?.school,
                    description:params?.description,
                    type:flow.type,
                    audio:params?.audio,
                    screen:params?.screen,
                    internet:params?.internet,
                    user:session?.user
                )

                if (!req.save()) {
                    flow.req = req
                    return error()
                }

                [req:req, requests:Request.requestFromTo(params.dateOfApplication, params.dateOfApplication).list()]
            }.to "hours"
        }

        hours {
            on("confirm") {
                //add to current request datashow selected
                flow.req.datashow = params.int("datashow")
                flow.req.save()

                //add hours to request
                def blocks = params.blocks

                if (blocks) {
                    blocks.each { block ->
                        flow.req.addToHours(new Hour(block:block))
                    }

                    flash.message = "request.saved"
                } else {
                    return error()
                }


            }.to "done"

            on("delete") {
                flow.req.delete()
            }.to "done"
        }

        done {
            redirect controller:"request", action:"list"
        }
    }

    def editRequestFlow = {
        init {
            action {
                def req = Request.findByIdAndUser(params.int("id"), session?.user)

                if (!req) {
                    response.sendError 404
                }

                if (req.status != "pending") {
                    flash.message = "access.denied.request.already.attended"
                    return done()
                }

                [req:req]
            }

            on("success").to "edit"
            on("done").to "done"
        }

        edit {
            on("confirm") {
                flow.req.dateOfApplication = parseDate(params?.dateOfApplication)
                flow.req.classroom = params?.classroom
                flow.req.school = params?.school
                flow.req.description = params?.description
                flow.req.audio = params?.audio
                flow.req.screen = params?.screen
                flow.req.internet = params?.internet

                if (!flow.req.save()) {
                    flow.req = flow.req
                    return error()
                }

                [req:flow.req, requests:Request.requestFromTo(params.dateOfApplication, params.dateOfApplication).list()]

            }.to("hours")
        }

        hours {
            on("confirm") {
                def blocks = params.list("blocks")

                if (blocks) {
                    //before add new block or blocks delete block linked to this request
                    def query = Hour.where {
                        request == flow.req
                    }

                    query.deleteAll()

                    //update datashow
                    if (params.int("datashow") != flow.req.datashow) {
                        flow.req.datashow = params.int("datashow")
                        flow.req.save()
                    }

                    //add new blocks
                    blocks.each { block ->
                        flow.req.addToHours(new Hour(block:block))
                    }
                } else {
                    return error()
                }
            }.to "done"

            on("cancel").to "done"
        }

        done {
            redirect controller:"request", action:"list"
        }
    }

    def show(Integer id) {
        def req = Request.get(id)

        if (!req) {
            response.sendError 404
        }

        [req:req]
    }

    def delete(Integer id) {
    	def req = Request.findByIdAndUser(id, session?.user)

    	if (!req) {
    		response.sendError 404
    	}

    	req.delete()

    	flash.message = "data.request.deleted"
    	redirect action:"list"
    }

    def updateStatus(Integer id) {
        def req = Request.get(id)

        if (!req) {
            response.sendError 404
            return false
        }

        //TODO:find a better solution for this scenario
        if (req.status == "pending") {
            req.status = "attended"
        } else if (req.status == "attended") {
            req.status = "absent"
        } else {
            req.status = "pending"
        }

        //TODO:find a better solution for this scenario
        if (!req.save(validate:false)) {
            req.errors.allErrors.each {
                print it
            }
            flash.message = "something.when.wrong.please.try.again"
            redirect action:"list"
            return false
        }

        flash.message = "request.successfuly.updated"

        if (params.path) {
            redirect action:"show", params:[id:id]
            return false
        }

        redirect action:"list", params:params
    }

    def disponability(String q) {
        def today = new Date().format("yyyy-MM-dd").toString()
        def requests = Request.requestFromTo((q) ?: today, (q) ?: today).findAllByStatus("pending")
        def day = (q) ? new Date().parse("yyyy-MM-dd", q)[Calendar.DAY_OF_WEEK] : new Date()[Calendar.DAY_OF_WEEK]
        def blocks

        switch(day) {
            case 7:
                blocks = grailsApplication.config.ni.edu.uccleon.saturday.blocks
                break
            case 1:
                blocks = grailsApplication.config.ni.edu.uccleon.sunday.blocks
                break
            default:
                blocks = grailsApplication.config.ni.edu.uccleon.blocks
        }

        [requests:requests, blocks:blocks, day:day]
    }

    //REPORTS
    def requestsBy(String from, String to, String type) {
        def f = parseDate(from)
        def t = parseDate(to)
        List results

        switch(type) {
            case "schools":
                results = (request.get) ? Request.requestsBy("school").list() : Request.requestsBy("school").requestFromTo(f, t).list()
                break
            case "classrooms":
                results = (request.get) ? Request.requestsBy("classroom").list() : Request.requestsBy("classroom").requestFromTo(f, t).list()
                break
            case "users":
                results = (request.get) ? Request.requestsBy("user").listByRole("user").list() : Request.requestsBy("user").listByRole("user").requestFromTo(f, t).list()
                break
            case "datashows":
                results = (request.get) ? Request.requestsBy("datashow").list() : Request.requestsBy("datashow").requestFromTo(f, t).list()
                break
            case "blocks":
                results = (request.get) ? Request.requestsByBlocks().list() : Request.requestsByBlocks().requestFromTo(f, t).list()
                break
        }

        [results:results, total:getTotal(results), type:type]
    }

    //LIST BY
    def listBy(String type) {
        def requests

        switch(type) {
            case "user":
                requests = Request.listByUser(session?.user).list(params)
                break
        }

        [requests:requests]
    }

    private parseDate(String date) {
        Date d

        if (!date) {
            return null
        }

        try {
            d = new Date().parse("yyyy-MM-dd", date)
        }
        catch(Exception e) {
            return null
        }

        d
    }

    private getTotal(List results) {
        def total = 0

        results.each {result ->
            total += result.count
        }

        total
    }

}

class updateStatusCommand {
    Integer id
    String status

    static constraints = {
        status inList:["pending", "attended", "absent"]
    }

    Request updateRequestStatus() {
        def req = Request.get(id)

        req.status = status

        //req.save()
    }
}
