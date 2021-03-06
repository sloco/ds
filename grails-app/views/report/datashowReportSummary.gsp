<g:applyLayout name="twoColumns">
    <head>
        <title>Sumario de uso de datashow</title>
        <r:require modules="bootstrap-css, bootstrap-responsive-css, jquery-ui, datepicker, app"/>
    </head>

    <content tag="main">
        <g:render template="nav"/>

        <g:if test="${results}">
            <table class="table table-hover">
                <colgroup>
                    <col span="1" style="width: 45%;">
                    <col span="1" style="width: 55%;">
                </colgroup>
            
                <thead>
                    <tr>
                        <th>Coordinacion</th>
                        <th>Cantidad</th>
                    </tr>
                </thead>

                <tbody>
                    <g:each in="${results}" var="result">
                        <tr>
                            <td>${result.school}</td>
                            <td>${result.count}</td>
                        </tr>
                    </g:each>
                    <tr>
                        <td>TOTAL</td>
                        <td>${results.count.sum()}</td>
                    </tr>
                </tbody>
            </table>
        </g:if>
        <g:else>
            <p>Sin datos que mostrar</p>
        </g:else>
    </content>
</g:applyLayout>
