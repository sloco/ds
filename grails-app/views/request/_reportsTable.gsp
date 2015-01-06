<g:if test="${type == 'resumen'}">
	<g:each in="${results}" var="result">
		<h4>${result.key}</h4>
		<table class="table">
			<tbody>
				<g:each in="${result.value}" var="data">
					<tr>
						<td width="1" style="border:0; vertical-align:middle;">${data.key}</td>
						<td style="border:0;">
							<div style="width:${data.value}px;" class="bar-chart">${data.value}</div>
						</td>
					</tr>
				</g:each>
			</tbody>
		</table>

		<p>
			Total ${totalRequestInYears[result.key]} Promedio <g:formatNumber number="${totalRequestInYears[result.key] / results[result.key].size()}" type="number" maxFractionDigits="2" roundingMode="HALF_DOWN"/>
		</p>
	</g:each>
</g:if>

<g:elseif test="${type == 'classrooms'}">
  <g:each in="${results}" var="result">
  	<h4>${result.key}</h4>
  	<table class="table borderless">
  		<colgroup>
				<col span="1" style="width: 15%;">
				<col span="1" style="width: 85%;">
			</colgroup>
  		<tbody>
		  	<g:each in="${result.value.sort { -it.value }}" var="data">
		  		<tr>
		  			<td style="vertical-align:middle;">${data.key}</td>
		  			<td>
		  				<div style="width:${data.value}px;" class="bar-chart">${data.value}</div>
		  			</td>
		  		</tr>
		  	</g:each>
	  	</tbody>
  	</table>
  </g:each>
</g:elseif>

<g:else>
		<table class="table table-hover">
		<thead>
			<tr>
				<th><ds:renderTitle title="${params.type}"/></th>
				<th>Cantidad</th>
			</tr>
		</thead>
		<tbody>
			<g:each in="${results}" var="result">
				<tr>
					<td>
						<g:if test="${params.type == 'users'}">
							<g:link action="listBy" params="[email:result.property.email, type:'user']">${result.property.fullName}</g:link>
						</g:if>
						<g:else>
							${result.property}
						</g:else>
					</td>
					<td>${result.count}</td>
				</tr>
			</g:each>
			<tr>
				<td>Total</td>
				<td>${total}</td>
			</tr>
		</tbody>
	</table>
</g:else>
