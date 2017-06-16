<g:applyLayout name="threeColumns">
    <head>
        <title>Reporte por soliciantes</title>
        <r:require modules="bootstrap-css, bootstrap-responsive-css, jquery-ui, datepicker, app"/>
    </head>

    <content tag="main">
        <g:render template="reportNavBar"/>

        <g:if test="${results}">
            <table class="table">
                <colgroup>
                    <col span="1" style="width: 45%;">
                    <col span="1" style="width: 55%;">
                </colgroup>

                <thead>
                    <tr>
                        <th>Solicitante</th>
                        <th>Cantidad</th>
                    </tr>
                </thead>

                <tbody>
                    <g:each in="${results}" var="result">
                        <tr>
                            <td>
                                <g:link
                                    action="coordinationReportPerApplicant"
                                    params="${params?.year ? [applicantID: result.applicantID, applicant: result.applicant, year: params.year] : [applicantID: result.applicantID, applicant: result.applicant]}">
                                    ${result.applicant}
                                </g:link>
                            </td>
                            <td>${result.quantity}</td>
                        </tr>
                    </g:each>
                    <tr>
                        <td>TOTAL</td>
                        <td>${results.quantity.sum()}</td>
                    </tr>
                </tbody>
            </table>
        </g:if>
        <g:else>
            <p>Sin datos que mostrar</p>
        </g:else>
    </content>

    <content tag="col1">
        <g:form action="reportByApplicant" autocomplete="off">
            <g:render template="years" model="[years: yearFilter.years]"/>

            <g:submitButton name="send" value="Filtrar" class="btn btn-primary btn-block"/>
        </g:form>
    </content>
</g:applyLayout>
