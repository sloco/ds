<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title><g:layoutTitle default="ds"/></title>
    <r:layoutResources/>
    <g:layoutHead/>
</head>
<body>
    <g:if test="${actionName == 'activity' && !session?.user}">
        <g:render template="/layouts/navbar"/>
    </g:if>

    <div class="container main">
        <div class="row">
            <g:layoutBody/>
        </div>
    </div>

    <r:layoutResources/>
    <script>
        (function(i,s,o,g,r,a,m){i['GoogleAnalyticsObject']=r;i[r]=i[r]||function(){
        (i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),
        m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)
        })(window,document,'script','https://www.google-analytics.com/analytics.js','ga');

        ga('create', 'UA-96342818-1', 'auto');
        ga('send', 'pageview');
    </script>
</body>
</html>