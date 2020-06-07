<!DOCTYPE html>
<html lang="en">
<head>
    <script src="https://cdn.jsdelivr.net/npm/heatmapjs@2.0.2/heatmap.min.js"></script>
    <script src="https://cdn.rawgit.com/bpmn-io/bower-bpmn-js/v0.22.1/dist/bpmn-navigated-viewer.js"></script>
    <script src="https://cdn.rawgit.com/ded/reqwest/v2.0.5/reqwest.js"></script>
    <link rel="stylesheet" href="https://stackpath.bootstrapcdn.com/bootstrap/4.5.0/css/bootstrap.min.css" integrity="sha384-9aIt2nRpC12Uk9gS9baDl411NQApFmC26EwAOH8WgZl5MYYxFfc+NcPb1dKGj7Sk" crossorigin="anonymous">

    <meta charset="utf-8">
    <title>Business Process Report</title>
    <style>
        body{
            zoom: 80%;
        }
        #canvas {
            width: 100%;
            height: 1000px;
            border: solid 1px #888;
            background: white;
        }
        .report-area {
            width: 500px;
        }

    </style>
</head>
<body>

<div >
    <div class="report-area">
        <label for="processExecutionSelect">Выберите процесса</label>
        <select class="form-control" id="processExecutionSelect" onchange="updateReport(this.value)">

        </select>
        <a  class="btn btn-primary" href="#" id="report-link" role="button">Скачать отчет</a>
    </div>
</div>


<div id="canvas">
</div>

<script>
    window.onload = function() {


        // load diagram

    }

    // const stats = {
    //   'START_PROCESS': 10,
    //   'Activity_0wjlrf0': 5,
    //   'Activity_19adocy': 5,
    //   'Activity_0dce88g': 18,
    //   'SCAN_OK': 10,
    //   'END_PROCESS': 5
    // }

    ///////// helpers

    /**
     * Small helper to load a diagram from a given url
     * and pass the result to a callback as (err, resultText).
     */
    function loadDiagram(url, callback) {



        // we're using reqwest to load the diagram;
        reqwest({
            url: url,
            crossOrigin: true,
            success: function(data) {
                callback(null, data);
            },
            error: callback
        });
    }

    /// load heatmap after the bpmn is loaded
    function plotHeatmap(bpmnJS, processExecInfo) {
        console.log('test0')

        var statsUrl = 'http://localhost:8080/tasks/queue/count?processInstanceId=' + processExecInfo.processInstanceId;
        var stats;
        reqwest({
            url: statsUrl,
            crossOrigin: true,
            success: function (data) {
                console.log(data) // as stats
                setHeatMap(data);
            }

        });
        console.log('test')
        console.log(stats)

    }

    function setHeatMap (stats) {
        var heatmap = h337.create({
            container: document.getElementById('canvas')
        });
        var data = [];
        var registry = bpmnJS.get('elementRegistry');
        var canvas = bpmnJS.get('canvas');
        for (var i in registry.getAll()) {
            var element = registry.getAll()[i];
            if (stats[element.id] != null) {
                // don't ask how we got to this calc. lots of tries. The 300/232 is the factor of the modeler available area and used.
                const rect = canvas.getGraphics(element).getBoundingClientRect();
                const x = (rect.x + rect.width / 2);
                const y = (rect.y + rect.height / 2);

                data.push({
                    x: x.toFixed(0),
                    y: y.toFixed(0),
                    value: stats[element.id] * 1.5
                });
            }
        }

        heatmap.setData({
            max: 20,
            data: data
        });
    }


    ///////// example

    // the diagram to load and display
    // var diagramUrl = 'https://rawgit.com/bpmn-io/bpmn-js-examples/master/overlays/resources/qr-code.bpmn';


    // bpmn-js instance mounted to #canvas element
    var bpmnJS = new BpmnJS({
        container: '#canvas'
    });

    var loadDiagramWrapped = function loadDiagramWrap (processExecInfo) {
        var diagramUrl = 'http://localhost:8080/bpmn?deploymentId=' + processExecInfo.deploymentId + '&resourceId=' + processExecInfo.resourceId;
        loadDiagram(diagramUrl, function(err, result) {

            removeCanvasElements ();
            if (err) {
                alert('Could not load diagram: ' + (err.message || err));
                return;
            }

            // display diagram in viewer
            bpmnJS.importXML(result, function(err, warnings) {
                if (err) {
                    alert('diagram import error: ' + err);
                } else {
                    // add marker
                    plotHeatmap(bpmnJS, processExecInfo);
                }

                if (warnings.length) {
                    alert(warnings.length + ' diagram import warnings; check developer console (F12)');
                }


            });

        });
    }

    function removeCanvasElements () {
        Array.prototype.slice.call(document.getElementsByTagName('canvas')).forEach(
            function(item) {
                item.remove();
                // or item.parentNode.removeChild(item); for older browsers (Edge-)
            });
    }

    function getProcessExecutionList () {
        var processExecUrl = 'http://localhost:8080/generator/instance/list';
        reqwest({
            url: processExecUrl,
            crossOrigin: true,
            success: function (data) {
                console.log(data) // as stats
                setToProcessExecutionSelect(data);
                loadDiagramWrapped(data[0]);
                updateReportLink(data[0].processInstanceId);
            }
        });
    }

    function setToProcessExecutionSelect (ProcessExecInfo) {
        var select = document.getElementById('processExecutionSelect');
        ProcessExecInfo.forEach (function (arrayItem) {
            var opt = document.createElement('option');
            opt.value = arrayItem.processInstanceId;
            opt.innerHTML = arrayItem.processTemplateId + ' ' + arrayItem.startTime;
            select.appendChild(opt);
        });
    }

    function updateReport (processExecId) {
        var processExecUrl = 'http://localhost:8080/generator/instance?generatorProcessInstanceId=' + processExecId;
        reqwest({
            url: processExecUrl,
            crossOrigin: true,
            success: function (data) {
                console.log(data);
                loadDiagramWrapped(data);
                updateReportLink(processExecId);
            }
        });
    }

    function updateReportLink (processExecId) {
        const link = document.getElementById('report-link');
        link.setAttribute('href', "/download/report/" + processExecId);
    }

    getProcessExecutionList();


</script>
<script src="https://code.jquery.com/jquery-3.5.1.slim.min.js" integrity="sha384-DfXdz2htPH0lsSSs5nCTpuj/zy4C+OGpamoFVy38MVBnE+IbbVYUew+OrCXaRkfj" crossorigin="anonymous"></script>
<script src="https://cdn.jsdelivr.net/npm/popper.js@1.16.0/dist/umd/popper.min.js" integrity="sha384-Q6E9RHvbIyZFJoft+2mJbHaEWldlvI9IOYy5n3zV9zzTtmI3UksdQRVvoxMfooAo" crossorigin="anonymous"></script>
<script src="https://stackpath.bootstrapcdn.com/bootstrap/4.5.0/js/bootstrap.min.js" integrity="sha384-OgVRvuATP1z7JjHLkuOU7Xw704+h835Lr+6QL9UvYjZE3Ipu6Tp75j7Bh/kR0JKI" crossorigin="anonymous"></script>
</body>
</html>