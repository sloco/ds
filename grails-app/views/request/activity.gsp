<!doctype html>
<html lang="en">
<head>
	<meta charset="UTF-8">
	<meta name="layout" content="${(!session?.user) ? 'public' : 'main'}">
	<title>Actividad</title>
	<g:set var="mainStyle" value="bootstrap-css, bootstrap-responsive-css, bootstrap-dropdown, jquery-ui, datepicker, app"/>
	<g:set var="activityStyle" value="bootstrap-css, bootstrap-responsive-css"/>
	<r:require modules = "${!session?.user ? activityStyle : mainStyle}"/>
</head>
<body>
	<g:set var="datashows" value="${grailsApplication.config.ni.edu.uccleon.datashows}"/>
	<g:set var="dateSelected" value="${dateSelected.format('yyyy-MM-dd')}"/>

	<g:if test="${requests}">
		<h4>${requests.size()} solicitudes el ${dateSelected}</h4>

		<div class="row">
			<g:each in="${1..datashows}" var="datashow">
				<div class="span2">
					<h4>Datashow ${datashow}</h4>
					<g:each in="${1..blocks}" var="block">
						<p>
							<g:if test="${requests.find {it.datashow == datashow && it?.hours?.block?.contains(block - 1)}}">
								<g:findAll in="${requests}" expr="it.datashow == datashow && it?.hours?.block?.contains(block - 1)">
									<g:if test="${session?.user?.role == 'admin'}">
										<div class="pull-right" style="margin-bottom:-6px;">
											<div class="button-group">
												<g:link class="btn btn-mini" action="updateStatus" params="[id:it.id, path:actionName, status:'canceled', dateSelected:dateSelected]"><i class="icon-remove"></i></g:link>
												<g:link class="btn btn-mini" action="updateStatus" params="[id:it.id, path:actionName, status:'absent', dateSelected:dateSelected]"><i class="icon-hand-down"></i></g:link>
												<g:link class="btn btn-mini" action="updateStatus" params="[id:it.id, path:actionName, status:'attended', dateSelected:dateSelected]"><i class="icon-ok"></i></g:link>
											</div>
										</div>
									</g:if>
									<div class="well well-small ${(it.user.email == session?.user?.email) ? 'owner' : 'no-owner'}">
										<small>
											<div class="row">
												<div class="span2">
													<strong>${it.user.fullName.toLowerCase().tokenize(" ")*.capitalize().join(" ")}</strong>
													<br>
													<strong>${it.classroom}</strong>
													<br>
													<ds:blockToHour block="${block}" doapp="${day}"/>
												</div>
											</div>
										</small>
									</div>
								</g:findAll>
							</g:if>
							<g:else>
								<ds:blockToHour block="${block}" doapp="${day}"/>
							</g:else>
						</p>
					</g:each>
				</div>
			</g:each>
		</div>
	</g:if>
	<g:else>
		<h4>No hay actividad programada ${dateSelected}!</h4>
	</g:else>
</body>
</html>