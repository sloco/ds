<div class="row">
	<div class="span8">
		<g:hiddenField name="type" value="${req?.type ?: type}"/>

		<g:if test="${session?.user?.role in ['coordinador', 'asistente']}">
			<label for="user">Solicitado por</label>
			<g:select name="user.id" from="${users}" optionKey="id" optionValue="fullName" value="${req?.user?.email}" class="form-control"/>
		</g:if>

		<label for="dateOfApplication">Fecha de solicitud</label>
		<g:textField name="dateOfApplication" value="${(actionName == 'createRequest' && type == 'express') ? new Date().format('yyyy-MM-dd') : g.formatDate(date:req?.dateOfApplication, format:'yyyy-MM-dd')}" autocomplete="off"/>

		<!--classrooms-->
		<g:render template="userClassrooms"/>

		<!--schools-->
		<g:render template="userSchools" model="[userSchools:userSchools, req:req]"/>

		<label for="description">Observacion</label>
		<g:textArea name="description" value="${req?.description}" class="input-block-level" style="resize:vertical; max-height:200px;"/>
	</div>
	<div class="span2">
		<div class="checkbox"><g:checkBox name="audio" value="${req?.audio}"/> Parlantes</div>
		<div class="checkbox"><g:checkBox name="screen" value="${req?.screen}"/> Pantalla</div>
		<div class="checkbox"><g:checkBox name="internet" value="${req?.internet}"/> Internet</div>

		<br>
		<small>
			Administra <g:link controller="user" action="classrooms">aulas</g:link>,
			<g:link controller="user" action="schoolsAndDepartments">Coordinaciones o departamentos</g:link>
		</small>
	</div>
</div>

<!--SAD solution T.T-->
<g:javascript>
	window.ajaxURL = "${createLink(controller: 'request', action: 'getUserClassroomsAndSchools')}"
</g:javascript>