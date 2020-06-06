import groovy.json.*;
/**
 *  Hubigraph Line Graph Child App
 *
 *  Copyright 2020, but let's behonest, you'll copy it
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */

// Hubigraph Line Graph Changelog
// *****ALPHA BUILD
// v0.1 Initial release
// v0.2 My son added webpage efficiencies, reduced load on hubitat by 75%.
// v0.3 Loading Update; Removed ALL processing from Hub, uses websocket endpoint
// v0.5 Multiple line support
// v0.51 Select ANY device
// v0.60 Select AXIS to graph on
// v0.70 A lot more options
// v0.80 Added Horizontal Axis Formatting
// ****BETA BUILD
// v0.1 Added Hubigraph Tile support with Auto-add Dashboard Tile
// v0.2 Added Custom Device/Attribute Labels
// v0.3 Added waiting screen for initial graph loading & sped up load times
// v0.32 Bug Fixes
// V 1.0 Released (not Beta) Cleanup and Preview Enabled

    
// Credit to Alden Howard for optimizing the code.
 
def ignoredEvents() { return [ 'lastReceive' , 'reachable' , 
                         'buttonReleased' , 'buttonPressed', 'lastCheckinDate', 'lastCheckin', 'buttonHeld' ] }

def version() { return "v1.0" }

definition(
    name: "Hubigraph Line Graph",
    namespace: "tchoward",
    author: "Thomas Howard",
    description: "Hubigraph Line Graph",
    category: "",
    parent: "tchoward:Hubigraphs",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
    iconX3Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience@2x.png",
)


preferences {
    section ("test"){
       page(name: "mainPage", install: true, uninstall: true)
       page(name: "deviceSelectionPage")
       page(name: "graphSetupPage")
       page(name: "enableAPIPage")
       page(name: "disableAPIPage")
}
   

    mappings {
        path("/graph/") {
            action: [
                GET: "getGraph"
            ]
        }
    
        path("/getData/") {
            action: [
                GET: "getData"
            ]
        }
        
        path("/getOptions/") {
            action: [
                GET: "getOptions"
            ]
        }
        
        path("/getSubscriptions/") {
            action: [
                GET: "getSubscriptions"
            ]
        }
    }
}


def addContainer(containers, numPerRow){
    
    def html_ = """
            <div class = "mdl-grid "> 
    """
    containers.each{container->
        html_ += """<div class="mdl-cell mdl-cell--${12/numPerRow}-col-desktop mdl-cell--${8/numPerRow}-col-tablet mdl-cell--${4/numPerRow}-col-phone mdl-textfield mdl-js-textfield" style="" data-upgraded=",MaterialTextfield">"""
        html_ += container;
        html_ += """</div>"""
    }
    html_ += """</div>"""
         
    paragraph (html_.replace('\t', '').replace('\n', '').replace('  ', ''));
    
}

def addText(text){
    
    def html_ = "$text";
    
    return html_
}

def specialPageButton(title, page, width, icon){
    def html_ = """
        <button type="button" name="_action_href_${page}|${page}|1" class="btn btn-default btn-lg btn-block hrefElem  mdl-button--raised mdl-shadow--2dp mdl-button__icon" style="text-align:left;width:${width}">
            <span style="text-align:left;white-space:pre-wrap">
                ${title}
            </span>
            <ul class="nav nav-pills pull-right">
                <li><i class="material-icons">${icon}</i></li>
            </ul>
            <br>
            <span class="state-incomplete-text " style="text-align: left; white-space:pre-wrap"></span>
       </button>
    """.replace('\t', '').replace('\n', '').replace('  ', '');
    
    return html_;
}

def specialSection(String name, Closure code) {
    def id = name.replace(' ', '_');
    
    def titleHTML = """
        <div class="mdl-layout__header" style="display: block !important;">
            <div class="mdl-layout__header-row">
                <span class="mdl-layout__title" style="margin-left: -32px; font-size: 20px;">
                    ${name}
                </span>
            </div>
        </div>
    """;
    
    def modContent = """
    <div id=${id} style="display: none;"></div>
    <script>
        var sectionElem = jQuery('#${id}').parent().parent();
        
        /*hide default header*/
        sectionElem.css('display', 'none');

        var elem = sectionElem.parent();
        elem.addClass('mdl-card mdl-card-wide mdl-shadow--8dp');
        elem.css('width', '100%');
        elem.prepend('${titleHTML}');
    </script>
    """;
    
    modContent = modContent.replace('\t', '').replace('\n', '').replace('  ', '');
    
    section(modContent) {
        code.call();
    }
}

def specialSwitch(title, var, defaultVal, submitOnChange){
   
    def actualVal = settings[var] != null ? settings[var] : defaultVal;
    
   def html_ = """      
                    <div class="form-group">
                        <input type="hidden" name="${var}.type" value="bool">
                        <input type="hidden" name="${var}.multiple" value="false">
                    </div>
                    <label for="settings[${var}]"
                        class="mdl-switch mdl-js-switch mdl-js-ripple-effect mdl-js-ripple-effect--ignore-events is-upgraded ${actualVal ? "is-checked" : ""}  
                            data-upgraded=",MaterialSwitch,MaterialRipple">
                            <input name="checkbox[${var}]" id="settings[${var}]" class="mdl-switch__input 
                                ${submitOnChange ? "submitOnChange" : ""} "
                                    type="checkbox" 
                                ${actualVal ? "checked" : ""}>                
                            <div class="mdl-switch__label">${title}</div>    
                            <div class="mdl-switch__track"></div>
                            <div class="mdl-switch__thumb">
                                <span class="mdl-switch__focus-helper">
                                </span>
                            </div>
                            <span class="mdl-switch__ripple-container mdl-js-ripple-effect mdl-ripple--center" data-upgraded=",MaterialRipple">
                                <span class="mdl-ripple">
                                </span>
                            </span>
                    </label>
                    <input name="settings[${var}]" type="hidden" value="${actualVal}">
    """
    return html_.replace('\t', '').replace('\n', '').replace('  ', '');;
 }

def specialTextInput(title, var, submitOnChange){
     def html_ = """
        <div class="form-group">
            <input type="hidden" name="${var}.type" value="text">
            <input type="hidden" name="${var}.multiple" value="false">
        </div>
        <label for="settings[${var}]" class="control-label">
            <b>${title}</b>
        </label>
            <input type="text" name="settings[${var}]" 
                   class="mdl-textfield__input ${submitOnChange == "true" ? "submitOnChange" : ""} " 
                   value="${settings[var]}" placeholder="Click to set" id="settings[${var}]">
        """
     return html_.replace('\t', '').replace('\n', '').replace('  ', '');
}

def deviceSelectionPage() {
    def supported_attrs;
        
    dynamicPage(name: "deviceSelectionPage") {
        specialSection("Device Selection"){
            input "sensors", "capability.*", title: "Sensors", multiple: true, required: true, submitOnChange: true
        
            if (sensors){
                sensors.each {
                    sensor_events = it.events([max:250]).name;
                    supported_attrs = sensor_events.unique(false);           
                    paragraph getSubTitle(it.displayName);
                    input( type: "enum", name: "attributes_${it.id}", title: "Attributes to graph", required: true, multiple: true, options: supported_attrs, defaultValue: "1")
                }
            }
        }
    }
}

def fontSizeSelector(varname, label, defaultSize, min, max){
    
    def fontSize;
    def varFontSize = "${varname}_font"
    
    settings[varFontSize] = settings[varFontSize] ? settings[varFontSize] : defaultSize;
    
    def html = "";
    
    html += 
    """
    <table style="width:100%">
    <tr><td><label for="settings[${varFontSize}]" class="control-label">${label} Font Size</td>
        <td style="text-align:right; font-size:${settings[varFontSize]}px">Font Size: ${settings[varFontSize]}</td>
        </label>
    </tr>
    </table>
    <input type="range" min = "$min" max = "$max" name="settings[${varFontSize}]" class="mdl-slider submitOnChange " value="${settings[varFontSize]}" id="settings[${varFontSize}]">
    <div class="form-group">
        <input type="hidden" name="${varFontSize}.type" value="number">
        <input type="hidden" name="${varFontSize}.multiple" value="false">
    </div>
    """.replace('\t', '').replace('\n', '').replace('  ', '');
    
    paragraph html
    
    //input (type: "range", name: varFontSize, title: "${label}:<p style='font-size:${settings["$varFontSize"]}px'>Font Size: ${settings["$varFontSize"]}</p>", min: "2", max: "20", submitOnChange: true);
    
}

def colorSelector(varname, label, defaultColorValue, defaultTransparentValue){
    def html = ""
    def varnameColor = "${varname}_color";
    def varnameTransparent = "${varname}_color_transparent"
    def colorTitle = "${label} Color"
    def notTransparentTitle = "Transparent";
    def transparentTitle = "${label}: Transparent"
    
    settings[varnameColor] = settings[varnameColor] ? settings[varnameColor]: defaultColorValue;
    settings[varnameTransparent] = settings[varnameTransparent] ? settings[varnameTransparent]: defaultTransparentValue;
    
    def isTransparent = settings[varnameTransparent];
    
    html += 
    """
    <div style="display: flex; flex-flow: row wrap;">
        <div style="display: flex; flex-flow: row nowrap; flex-basis: 100%;">
            ${!isTransparent ? """<label for="settings[${varnameColor}]" class="control-label" style="flex-grow: 1">${colorTitle}</label>""" : """"""}
            <label for="settings[${varnameTransparent}]" class="control-label" style="width: auto;">${isTransparent ? transparentTitle: notTransparentTitle}</label>
        </div>
        ${!isTransparent ? """
            <div style="flex-grow: 1; flex-basis: 1px; padding-right: 8px;">
                <input type="color" name="settings[${varnameColor}]" class="mdl-textfield__input" value="${settings[varnameColor] ? settings[varnameColor] : defaultColorValue}" placeholder="Click to set" id="settings[${varnameColor}]" list="presetColors">
                  <datalist id="presetColors">
                    <option>#800000</option>
                    <option>#FF0000</option>
                    <option>#FFA500</option>
                    <option>#FFFF00</option>

                    <option>#808000</option>
                    <option>#008000</option>
                    <option>#00FF00</option>
                    
                    <option>#800080</option>
                    <option>#FF00FF</option>
                    
                    <option>#000080</option>
                    <option>#0000FF</option>
                    <option>#00FFFF</option>

                    <option>#FFFFFF</option>
                    <option>#C0C0C0</option>
                    <option>#000000</option>
                  </datalist>
            </div>
        """ : ""}
        <div class="submitOnChange">
            <input name="checkbox[${varnameTransparent}]" id="settings[${varnameTransparent}]" style="width: 27.6px; height: 27.6px;" type="checkbox" onmousedown="((e) => { jQuery('#${varnameTransparent}').val('${!isTransparent}'); })()" ${isTransparent ? 'checked' : ''} />
            <input id="${varnameTransparent}" name="settings[${varnameTransparent}]" type="hidden" value="${isTransparent}" />
        </div>
        <div class="form-group">
            <input type="hidden" name="${varnameColor}.type" value="color">
            <input type="hidden" name="${varnameColor}.multiple" value="false">

            <input type="hidden" name="${varnameTransparent}.type" value="bool">
            <input type="hidden" name="${varnameTransparent}.multiple" value="false">
        </div>
    </div>
    """.replace('\t', '').replace('\n', '').replace('  ', '');
    
    //paragraph html;
    return html;
}

def graphSetupPage(){
    def fontEnum = [["1":"1"], ["2":"2"], ["3":"3"], ["4":"4"], ["5":"5"], ["6":"6"], ["7":"7"], ["8":"8"], ["9":"9"], ["10":"10"], 
                    ["11":"11"], ["12":"12"], ["13":"13"], ["14":"14"], ["15":"15"], ["16":"16"], ["17":"17"], ["18":"18"], ["19":"19"], ["20":"20"]];  
    
    def colorEnum = [["#800000":"Maroon"], ["#FF0000":"Red"], ["#FFA500":"Orange"], ["#FFFF00":"Yellow"], ["#808000":"Olive"], ["#008000":"Green"],
                    ["#800080":"Purple"], ["#FF00FF":"Fuchsia"], ["#00FF00":"Lime"], ["#008080":"Teal"], ["#00FFFF":"Aqua"], ["#0000FF":"Blue"], ["#000080":"Navy"],
                    ["#000000":"Black"], ["#C0C0C0":"Gray"], ["#C0C0C0":"Silver"], ["#FFFFFF":"White"], ["transparent":"Transparent"]];
    dynamicPage(name: "graphSetupPage") {
        specialSection("General Options")
        {
            test = [];
            colorSelector("graph_background2", "Background", "#FFFFFF", false);
            
            input( type: "enum", name: "graph_update_rate", title: "Select graph update rate", multiple: false, required: true, options: [["-1":"Never"], ["0":"Real Time"], ["10":"10 Milliseconds"], ["1000":"1 Second"], ["5000":"5 Seconds"], ["60000":"1 Minute"], ["300000":"5 Minutes"], ["600000":"10 Minutes"], ["1800000":"Half Hour"], ["3600000":"1 Hour"]], defaultValue: "0")
            input( type: "enum", name: "graph_timespan", title: "Select Timespan to Graph", multiple: false, required: true, options: [["60000":"1 Minute"], ["3600000":"1 Hour"], ["43200000":"12 Hours"], ["86400000":"1 Day"], ["259200000":"3 Days"], ["604800000":"1 Week"]], defaultValue: "43200000")
            colorSelector("graph_background", "Background", "#FFFFFF", false);
            
            input( type: "bool", name: "graph_smoothing", title: "Smooth Graph Points", defaultValue: true);
            input( type: "enum", name: "graph_type", title: "Graph Type", defaultValue: "Line Graph", options: ["Line Graph", "Area Graph", "Scatter Plot"] )
            input( type: "bool", name: "graph_y_orientation", title: "Flip Graph to Vertical (Rotate 90 degrees)", defaultValue: false);
            input( type: "bool", name: "graph_z_orientation", title: "Reverse Data Order? (Flip Data left to Right)", defaultValue: false);
            input (type: "number", name: "graph_max_points", title: "Maximum number of Data Points? (Zero for ALL)", defaultValue: 0);
            
            
           
        }
        
        
        specialSection("Graph Title")
        {    
            input( type: "bool", name: "graph_show_title", title: "Show Title on Graph", defaultValue: false, submitOnChange: true);
            if (graph_show_title==true) {
                input( type: "text", name: "graph_title", title: "Input Graph Title", default: "Graph Title");
                fontSizeSelector("graph_title", "Title", 9, 2, 20);
                colorSelector("graph_title", "Title", "#000000", false);
                input( type: "bool", name: "graph_title_inside", title: "Put Title Inside Graph", defaultValue: false);
            }
        }
            
         specialSection("Graph Size")
         {    
            input( type: "bool", name: "graph_static_size", title: "Set size of Graph? (False = Fill Window)", defaultValue: false, submitOnChange: true);
            if (graph_static_size==true){
                input( type: "number", name: "graph_h_size", title: "Horizontal dimension of the graph", defaultValue: "800", range: "100..3000");
                input( type: "number", name: "graph_v_size", title: "Vertical dimension of the graph", defaultValue: "600", range: "100..3000");
            }
         }
        
          specialSection("Horizontal Axis")
         { 
            //Axis
            fontSizeSelector("graph_haxis", "Horizonal Axis", 9, 2, 20);
            colorSelector("graph_hh", "Horizonal Header", "#C0C0C0", false);
            colorSelector("graph_ha", "Horizonal Axis", "#C0C0C0", false);
            input( type: "number", name: "graph_h_num_grid", title: "Num Horizontal Gridlines (blank for auto)", defaultValue: "", range: "0..100");
            
            input( type: "bool", name: "dummy", title: "Show String Formatting Help", defaultValue: false, submitOnChange: true);
            if (dummy == true){
                paragraph_ = "<table>"
                paragraph_ += getTableRow("<b>Name", "<b>Format" ,"<b>Result</b>", "");
                paragraph_ += getTableRow("Year", "Y", "2020", "");
                paragraph_ += getTableRow("Month Number", "M", "12", "");
                paragraph_ += getTableRow("Month Name ", "MMM", "Feb", "");
                paragraph_ += getTableRow("Month Full Name", "MMMM", "February", "");
                paragraph_ += getTableRow("Day of Month", "d", "February", "");
                paragraph_ += getTableRow("Day of Week", "EEE", "Mon", "");
                paragraph_ += getTableRow("Day of Week", "EEEE", "Monday", "");
                paragraph_ += getTableRow("Period", "a", "AM or PM", "");
                paragraph_ += getTableRow("Hour (12)", "h", "1..12", "");
                paragraph_ += getTableRow("Hour (12)", "hh", "01..12", "");
                paragraph_ += getTableRow("Hour (24)", "H", "1..23", "");
                paragraph_ += getTableRow("Hour (24)", "HH", "01..23", "");
                paragraph_ += getTableRow("Minute", "m", "1..59", "");
                paragraph_ += getTableRow("Minute", "mm", "01..59", "");
                paragraph_ += getTableRow("Seconds", "s", "1..59", "");
                paragraph_ += getTableRow("Seconds", "ss", "01..59", "");
                paragraph_ += "</table>"
                paragraph paragraph_
                paragraph_ = /<b>Example: "EEEE, MMM d, Y hh:mm:ss a" = "Monday, June 2, 2020 08:21:33 AM"/
                paragraph_ += "</b>"
                paragraph paragraph_
            }
            input( type: "string", name: "graph_h_format", title: "Horizontal Axis Format", defaultValue: "", submitOnChange: true);
            if (graph_h_format){
                today = new Date();
                paragraph "Horizontal Axis Sample: ${today.format(graph_h_format)}"
            }
         }
            
         specialSection("Vertical Axis")
         { 
            fontSizeSelector("graph_vaxis", "Title", 9, 2, 20);
            colorSelector("graph_vh", "Vertical Header", "#000000", false);
            colorSelector("graph_va", "Vertical Axis", "#C0C0C0", false);
         }
            
        specialSection("Left Axis"){  
            input( type: "decimal", name: "graph_vaxis_1_min", title: "Minimum for left axis (blank for auto)", defaultValue: "");
            input( type: "decimal", name: "graph_vaxis_1_max", title: "Maximum for left axis (blank for auto)", defaultValue: "");
            input( type: "number", name: "graph_vaxis_1_num_lines", title: "Num gridlines (blank for auto)", defaultValue: "", range: "0..100");
            input( type: "bool", name: "graph_show_left_label", title: "Show Left Axis Label on Graph", defaultValue: false, submitOnChange: true);
            if (graph_show_left_label==true){
                input( type: "text", name: "graph_left_label", title: "Input Left Axis Label", default: "Left Axis Label");
                fontSizeSelector("graph_left", "Left Axis", 9, 2, 20);
                colorSelector("graph_left", "Left Axis", "#FFFFFF", false);
            }
        }
           
        specialSection("Right Axis"){
            input( type: "decimal", name: "graph_vaxis_2_min", title: "Minimum for right axis (blank for auto)", defaultValue: "", range: "");
            input( type: "decimal", name: "graph_vaxis_2_max", title: "Maximum for right axis (blank for auto)", defaultValue: "", range: "");
            input( type: "number", name: "graph_vaxis_2_num_lines", title: "Num gridlines (blank for auto) -- Must be greater than num tics to be effective", defaultValue: "", range: "0..100");
            input( type: "bool", name: "graph_show_right_label", title: "Show Right Axis Label on Graph", defaultValue: false, submitOnChange: true);
            if (graph_show_right_label==true){
                input( type: "text", name: "graph_right_label", title: "Input Right Axis Label", default: "Right Axis Label");
                fontSizeSelector("graph_right", "Right Axis", 9, 2, 20);
                colorSelector("graph_right", "Right Axis", "#FFFFFF", false);
             }
        }  
            //Legend
        specialSection("Legend"){
            def legendPosition = [["top": "Top"], ["bottom":"Bottom"], ["in": "Inside Top"]];
            def insidePosition = [["start": "Left"], ["center": "Center"], ["end": "Right"]];
            input( type: "bool", name: "graph_show_legend", title: "Show Legend on Graph", defaultValue: false, submitOnChange: true);
            if (graph_show_legend==true){
                fontSizeSelector("graph_legend", "Legend Font", 9, 2, 20);
                colorSelector("graph_legend", "Legend", "#000000", false);
                input( type: "enum", name: "graph_legend_position", title: "Legend Position", defaultValue: "Bottom", options: legendPosition);
                input( type: "enum", name: "graph_legend_inside_position", title: "Legend Justification", defaultValue: "center", options: insidePosition);
                
            }
        }
        
        specialSection("Lines"){

            //Get the total number of devices
            state.num_devices = 0;
            sensors.each { sensor ->
                settings["attributes_${sensor.id}"].each { attribute ->
                    state.num_devices++;
                }
            }
            def availableAxis = [["0" : "Left Axis"], ["1": "Right Axis"]];
            if (state.num_devices == 1) {
                    availableAxis = [["0" : "Left Axis"], ["1": "Right Axis"], ["2": "Both Axes"]]; 
            }
            
            
            //Line
            cnt = 1;
            sensors.each { sensor ->        
                settings["attributes_${sensor.id}"].each { attribute ->
                        paragraph getSubTitle("${sensor.displayName}: ${attribute}");
                        input( type: "enum", name:   "graph_axis_number_${sensor.id}_${attribute}", title: "Graph Axis Number", defaultValue: "0", options: availableAxis);
                        colorSelector("graph_line_${sensor.id}_${attribute}", "${sensor}: ${attribute} Line", getColorCode(cnt), false);
                        input( type: "enum", name:   "graph_line_thickness_${sensor.id}_${attribute}", title: "Line Thickness", defaultValue: "2", options: fontEnum); 
                        input( type: "string", name: "graph_name_override_${sensor.id}_${attribute}", title: "Override Device Name -- use %deviceName% for DEVICE and %attributeName% for ATTRIBUTE", defaultValue: "%deviceName%: %attributeName%");
                        cnt += 1;
                    }
                }
            
        }
    }
}

def disableAPIPage() {
    dynamicPage(name: "disableAPIPage") {
        section() {
            if (state.endpoint) {
                revokeAccessToken();
                state.endpoint = null
            }
            paragraph "Token revoked. Click done to continue."
        }
    }
}

def enableAPIPage() {
    dynamicPage(name: "enableAPIPage", title: "") {
        section() {
            if(!state.endpoint) initializeAppEndpoint();
            paragraph "Token created. Click done to continue."
        }
    }
}

def getTimeString(string_){
    switch (string_.toInteger()){
        case -1: return "Never";
        case 0: return "Real Time"; 
        case 1000:return "1 Second"; 
        case 5000:return "5 Seconds"; 
        case 60000:return "1 Minute"; 
        case 300000:return "5 Minutes"; 
        case 600000:return "10 Minutes"; 
        case 1800000:return "Half Hour";
        case 3600000:return "1 Hour";
    }
    logDebug("NOT FOUND");
}

def getTimsSpanString(string_){
    switch (string_.toInteger()){
        case -1: return "Never";
        case 0: return "Real Time"; 
        case 1000:return "1 Second"; 
        case 5000:return "5 Seconds"; 
        case 60000:return "1 Minute"; 
        case 3600000:return "1 Hour";
        case 43200000: return "12 Hours";
        case 86400000: return "1 Day";
        case 259200000: return "3 Days";
        case 604800000: return "1 Week";
    }
    logDebug("NOT FOUND");
}

def getColorString(string_){
    switch (string_){
        case "#800000": return "Maroon";
        case "#FF0000": return "Red";
        case "#FF0000": return "Orange";
        case "#FFFF00": return "Yellow";
        case "#808000": return "Olive";
        case "#008000": return "Green";
        case "#800080": return "Purple";
        case "#FF00FF": return "Fuchsia";
        case "#00FF00": return "Lime";
        case "#008080": return "Teal";
        case "#00FFFF": return "Aqua";
        case "#0000FF": return "Blue";
        case "#000080": return "Navy";
        case "#000000": return "Black";
        case "#C0C0C0": return "Gray";
        case "#C0C0C0": return "Silver";
        case "#FFFFFF": return "White";
        case "rgba(255, 255, 255, 0)": return "Transparent";   
        
    }
    
}

def loadPreview(){
  def html = ""
    
    html+= """
<iframe id="preview" style="width: 100%; height: 100%; background-image: url('data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAIAAAACCAIAAAD91JpzAAAEq2lUWHRYTUw6Y29tLmFkb2JlLnhtcAAAAAAAPD94cGFja2V0IGJlZ2luPSLvu78iIGlkPSJXNU0wTXBDZWhpSHpyZVN6TlRjemtjOWQiPz4KPHg6eG1wbWV0YSB4bWxuczp4PSJhZG9iZTpuczptZXRhLyIgeDp4bXB0az0iWE1QIENvcmUgNS41LjAiPgogPHJkZjpSREYgeG1sbnM6cmRmPSJodHRwOi8vd3d3LnczLm9yZy8xOTk5LzAyLzIyLXJkZi1zeW50YXgtbnMjIj4KICA8cmRmOkRlc2NyaXB0aW9uIHJkZjphYm91dD0iIgogICAgeG1sbnM6ZXhpZj0iaHR0cDovL25zLmFkb2JlLmNvbS9leGlmLzEuMC8iCiAgICB4bWxuczp0aWZmPSJodHRwOi8vbnMuYWRvYmUuY29tL3RpZmYvMS4wLyIKICAgIHhtbG5zOnBob3Rvc2hvcD0iaHR0cDovL25zLmFkb2JlLmNvbS9waG90b3Nob3AvMS4wLyIKICAgIHhtbG5zOnhtcD0iaHR0cDovL25zLmFkb2JlLmNvbS94YXAvMS4wLyIKICAgIHhtbG5zOnhtcE1NPSJodHRwOi8vbnMuYWRvYmUuY29tL3hhcC8xLjAvbW0vIgogICAgeG1sbnM6c3RFdnQ9Imh0dHA6Ly9ucy5hZG9iZS5jb20veGFwLzEuMC9zVHlwZS9SZXNvdXJjZUV2ZW50IyIKICAgZXhpZjpQaXhlbFhEaW1lbnNpb249IjIiCiAgIGV4aWY6UGl4ZWxZRGltZW5zaW9uPSIyIgogICBleGlmOkNvbG9yU3BhY2U9IjEiCiAgIHRpZmY6SW1hZ2VXaWR0aD0iMiIKICAgdGlmZjpJbWFnZUxlbmd0aD0iMiIKICAgdGlmZjpSZXNvbHV0aW9uVW5pdD0iMiIKICAgdGlmZjpYUmVzb2x1dGlvbj0iNzIuMCIKICAgdGlmZjpZUmVzb2x1dGlvbj0iNzIuMCIKICAgcGhvdG9zaG9wOkNvbG9yTW9kZT0iMyIKICAgcGhvdG9zaG9wOklDQ1Byb2ZpbGU9InNSR0IgSUVDNjE5NjYtMi4xIgogICB4bXA6TW9kaWZ5RGF0ZT0iMjAyMC0wNi0wMlQxOTo0NzowNS0wNDowMCIKICAgeG1wOk1ldGFkYXRhRGF0ZT0iMjAyMC0wNi0wMlQxOTo0NzowNS0wNDowMCI+CiAgIDx4bXBNTTpIaXN0b3J5PgogICAgPHJkZjpTZXE+CiAgICAgPHJkZjpsaQogICAgICBzdEV2dDphY3Rpb249InByb2R1Y2VkIgogICAgICBzdEV2dDpzb2Z0d2FyZUFnZW50PSJBZmZpbml0eSBQaG90byAxLjguMyIKICAgICAgc3RFdnQ6d2hlbj0iMjAyMC0wNi0wMlQxOTo0NzowNS0wNDowMCIvPgogICAgPC9yZGY6U2VxPgogICA8L3htcE1NOkhpc3Rvcnk+CiAgPC9yZGY6RGVzY3JpcHRpb24+CiA8L3JkZjpSREY+CjwveDp4bXBtZXRhPgo8P3hwYWNrZXQgZW5kPSJyIj8+IC4TuwAAAYRpQ0NQc1JHQiBJRUM2MTk2Ni0yLjEAACiRdZE7SwNBFEaPiRrxQQQFLSyCRiuVGEG0sUjwBWqRRPDVbDYvIYnLboIEW8E2oCDa+Cr0F2grWAuCoghiZWGtaKOy3k2EBIkzzL2Hb+ZeZr4BWyippoxqD6TSGT0w4XPNLyy6HM/UYqONfroU1dBmguMh/h0fd1RZ+abP6vX/uYqjIRI1VKiqEx5VNT0jPCk8vZbRLN4WblUTSkT4VLhXlwsK31p6uMgvFseL/GWxHgr4wdYs7IqXcbiM1YSeEpaX404ls+rvfayXNEbTc0HJnbI6MAgwgQ8XU4zhZ4gBRiQO0YdXHBoQ7yrXewr1s6xKrSpRI4fOCnESZOgVNSvdo5JjokdlJslZ/v/11YgNeovdG31Q82Sab93g2ILvvGl+Hprm9xHYH+EiXapfPYDhd9HzJc29D84NOLssaeEdON+E9gdN0ZWCZJdli8Xg9QSaFqDlGuqXip797nN8D6F1+aor2N2DHjnvXP4Bhcln9Ef7rWMAAAAJcEhZcwAACxMAAAsTAQCanBgAAAAXSURBVAiZY7hw4cL///8Z////f/HiRQBMEQrfQiLDpgAAAABJRU5ErkJggg=='); background-size: 25px; background-repeat: repeat; image-rendering: pixelated;" src="${state.localEndpointURL}graph/?access_token=${state.endpointSecret}"></iframe>
<script>
function resize() {
    const box = jQuery('#formApp')[0].getBoundingClientRect();
    const h = box.width * 0.75;
    const w = box.width * 0.95;   
    jQuery('#preview').css('height', h);
    jQuery('#preview').css('width', w);
}

resize();

jQuery(window).on('resize', () => {
    resize();
});
</script>
"""
}

def mainPage() {
    dynamicPage(name: "mainPage") {        
         if (!state.endpoint) {
             specialSection("Please set up OAuth API"){
                href name: "enableAPIPageLink", title: "Enable API", description: "", page: "enableAPIPage"    
             }
            } else {
                specialSection("Graph Options"){
                    objects = [];
                    objects << specialPageButton("Select Device/Data", "deviceSelectionPage", "100%", "vibration");
                    objects << specialPageButton("Configure Graph", "graphSetupPage", "100%", "poll");
                    addContainer(objects, 1);
                }
                specialSection("Local Graph URL"){
                    addContainer(["${state.localEndpointURL}graph/?access_token=${state.endpointSecret}"], 1);
                }
             
                if (sensors){
                    specialSection("Sensors"){
                            rows = [];
                            rows << addText("<b><u>DEVICE</b></u>");
                            rows << addText("<b><u>ATTRIBUTES</b></u>");            
                            sensors.each { sensor ->
                                settings["attributes_${sensor.id}"].each { attribute_ ->                                        
                                    rows << addText("$sensor")
                                    rows << addText("$attribute_");
                                }
                            }
                            addContainer(rows, 2);                        
                    }
                    
                    if (graph_update_rate){
                        specialSection("Preview"){
                             paragraph loadPreview()
                        }
                    } //if (graph_update_rate)
                } // if sensors                            
            } //else
        
        specialSection("Hubigraph Tile Installation"){
            objects = [];
            objects << specialSwitch("Install Hubigraph Tile Device?", "install_device", false, true);
            if (install_device==true){ 
                 objects << specialTextInput("Name for HubiGraph Tile Device", "device_name", "false");
            }
            addContainer(objects, 1);
        }
        specialSection("Hubigraph Application"){
            if (state.endpoint){
                paragraph getSubTitle("Application Name");
                addContainer([specialTextInput("Rename the Application?", "app_name", "false")], 1);
                paragraph getSubTitle("Debugging");
                addContainer([specialSwitch("Enable Debug Logging?", "debug", false, false)], 1);
                
                paragraph getSubTitle("Disable Oauth Authorization");
                addContainer([specialPageButton("Disable API", "disableAPIPage", "100%", "cancel")], 1);
                //href "disableAPIPage", title: "Disable API", description: ""
            }
        }
       
        
    }
}

def logDebug(str){
    if (debug==true){
        log.debug(str);   
    }
}

def createHubiGraphTile() {
	log.info "Creating HubiGraph Child Device"
    
    def childDevice = getChildDevice("HUBIGRAPH_${app.id}");     
    logDebug(childDevice);
   
    if (!childDevice) {
        if (!device_name) device_name="Dummy Device";
        logDebug("Creating Device $device_name");
    	childDevice = addChildDevice("tchoward", "Hubigraph Tile Device", "HUBIGRAPH_${app.id}", null,[completedSetup: true, label: device_name]) 
        log.info "Created HTTP Switch [${childDevice}]"
        
        //Send the html automatically
        childDevice.setGraph("${state.localEndpointURL}graph/?access_token=${state.endpointSecret}");
        log.info "Sent setGraph: ${state.localEndpointURL}graph/?access_token=${state.endpointSecret}"
	}
    else {
    	
        childDevice.label = device_name;
        log.info "Label Updated to [${device_name}]"
        
        //Send the html automatically
        childDevice.setGraph("${state.localEndpointURL}graph/?access_token=${state.endpointSecret}");
        log.info "Sent setGraph: ${state.localEndpointURL}graph/?access_token=${state.endpointSecret}"
	}

}

def getLine(){	  
	def html = "<hr style='background-color:#1A77C9; height: 1px; border: 0;'>"
    html
}

def getTableRow(col1, col2, col3, col4){
     def html = "<tr><td width='40%'>$col1</td><td width='30%'>$col2</td><td width='20%'>$col3</td><td width='10%'>$col4</td></tr>"  
     html
}

def getTitle(myText=""){
    def html = "<div class='row-full' style='background-color:#1A77C9;color:white;font-weight: bold; text-align: center; font-size: 20px '>"
    html += "${myText}</div>"
    html
}

def getSubTitle(myText=""){
    def html = """
        <div class="mdl-layout__header" style="display: block !important;">
        <div class="mdl-layout__header-row">
        <span class="mdl-layout__title" style="margin-left: -32px; font-size: 9px;">
                   <h3>${myText}</h3>
        </span>
        </div>
        </div>
    """.replace('\t', '').replace('\n', '').replace('  ', '');
                
    return html
}

def installed() {
    logDebug "Installed with settings: ${settings}"
    initialize()
}

def uninstalled() {
    if (state.endpoint) {
        try {
            logDebug "Revoking API access token"
            revokeAccessToken()
        }
        catch (e) {
            log.warn "Unable to revoke API access token: $e"
        }
    }
    removeChildDevices(getChildDevices());
}

private removeChildDevices(delete) {
	delete.each {deleteChildDevice(it.deviceNetworkId)}
}

def updated() {
    app.updateLabel(app_name);
    
    if (install_device == true){
        createHubiGraphTile();
    }
    
}

def initialize() {
   updated();
}

private buildData() {
    def resp = [:]
    def today = new Date();
    def then = new Date();
    
    use (groovy.time.TimeCategory) {
           then -= Integer.parseInt(graph_timespan).milliseconds;
    }
    
    logDebug("Initializing:: Device = $sensor  Attribute = $attribute from $then to $today");
    
    if(sensors) {
        sensors.each { sensor ->
            resp[sensor.id] = [:];
            settings["attributes_${sensor.id}"].each { attribute ->
                def respEvents = [];    
                
                logDebug("Checking:: $sensor.displayName: $attribute id:$sensor.id");
                def reg = ~/[a-z,A-Z]+/;
            
                respEvents << sensor.statesSince(attribute, then, [max: 50000]).collect{[ date: it.date.getTime(), value: Float.parseFloat(it.value - reg ) ]}
                respEvents = respEvents.flatten();
                respEvents = respEvents.reverse();
                
                //graph_max_ponts
                if (graph_max_points > 0) {
                    reduction = (int) Math.ceil((float)respEvents.size() / graph_max_points);
                    respEvents = respEvents.collate(reduction).collect{ group -> 
                        group.inject([ date: 0, value: 0.0f ]){ col, it -> 
                            col.date += it.date / group.size();
                            col.value += + it.value / group.size();
                            return col;
                        } 
                    };                             
                }                    
                
                resp[sensor.id][attribute] = respEvents;
            }
        }
    }
    logDebug("Done");  
    
    return resp;
}

def getChartOptions(){
    
    def options = [
        "graphReduction": graph_max_points,
        "graphTimespan": Integer.parseInt(graph_timespan),
        "graphUpdateRate": Integer.parseInt(graph_update_rate),
        "graphOptions": [
            "width": graph_static_size ? graph_h_size : "100%",
            "height": graph_static_size ? graph_v_size: "100%",
            "chartArea": [ "width": graph_static_size ? graph_h_size : "80%", "height": graph_static_size ? graph_v_size: "80%"],
            "hAxis": ["textStyle": ["fontSize": graph_haxis_font, 
                                    "color": graph_hh_color_transparent ? "transparent" : graph_hh_color ], 
                      "gridlines": ["color": graph_ha_color_transparent ? "transparent" : graph_ha_color, 
                                    "count": graph_h_num_grid != "" ? graph_h_num_grid : null
                                   ],
                      "format":     graph_h_format==""?"":graph_h_format                          
                     ],
            "vAxis": ["textStyle": ["fontSize": graph_vaxis_font, 
                                    "color": graph_vh_color_transparent ? "transparent" : graph_vh_color], 
                      "gridLines": ["color": graph_va_color_transparent ? "transparent" : graph_va_color],
                     ],
            "vAxes": [
                0: ["title" : graph_show_left_label ? graph_left_label: null,  
                    "titleTextStyle": ["color": graph_left_color_transparent ? "transparent" : graph_left_color, "fontSize": graph_left_font],
                    "viewWindow": ["min": graph_vaxis_1_min != "" ?  graph_vaxis_1_min : null, 
                                   "max":  graph_vaxis_1_max != "" ?  graph_vaxis_1_max : null],
                    "gridlines": ["count" : graph_vaxis_1_num_tics != "" ? graph_vaxis_1_num_tics : null ],
                    "minorGridlines": ["count" : 0]
                   ],
                
                1: ["title": graph_show_right_label ? graph_right_label : null,
                    "titleTextStyle": ["color": graph_right_color_transparent ? "transparent" : graph_right_color, "fontSize": graph_right_font],
                    "viewWindow": ["min": graph_vaxis_2_min != "" ?  graph_vaxis_2_min : null, 
                                   "max":  graph_vaxis_2_max != "" ?  graph_vaxis_2_max : null],
                    "gridlines": ["count" : graph_vaxis_2_num_tics != "" ? graph_vaxis_2_num_tics : null ],
                    "minorGridlines": ["count" : 0]
                    ]
                
            ],
            "legend": !graph_show_legend ? ["position": "none"] : ["position": graph_legend_position,  
                                                                   "alignment": graph_legend_inside_position, 
                                                                   "textStyle": ["fontSize": graph_legend_font, 
                                                                                 "color": graph_legend_color_transparent ? "transparent" : graph_legend_color]],
            "backgroundColor": graph_background_color_transparent ? "transparent" : graph_background_color,
            "curveType": !graph_smoothing ? "" : "function",
            "title": !graph_show_title ? "" : graph_title,
            "titleTextStyle": !graph_show_title ? "" : ["fontSize": graph_title_font, "color": graph_title_color_transparent ? "transparent" : graph_title_color],
            "titlePosition" :  graph_title_inside ? "in" : "out",
            "interpolateNulls": true, //for null vals on our chart
            "orientation" : graph_y_orientation == true ? "vertical" : "horizontal",
            "reverseCategories" : graph_x_orientation,
            "series": [],
            
        ]
    ];
    
    //add colors and thicknesses
    sensors.each { sensor ->
        settings["attributes_${sensor.id}"].each { attribute ->
            def axis = Integer.parseInt(settings["graph_axis_number_${sensor.id}_${attribute}"]);
            def text_color = settings["graph_line_${sensor.id}_${attribute}_color"];
            def text_color_transparent = settings["graph_line_${sensor.id}_${attribute}_color_transparent"];
            def line_thickness = settings["graph_line_thickness_${sensor.id}_${attribute}"];
            
            def annotations = [
                "targetAxisIndex": axis, 
                "color": text_color_transparent ? "transparent" : text_color,
                "lineWidth": line_thickness
            ];
            
            options.graphOptions.series << annotations;  
        }
    }    
    
    return options;
}
        
def getDrawType(){
    switch (graph_type){
        case "Line Graph": return "google.visualization.LineChart";
        case "Area Graph": return "google.visualization.AreaChart";
        case "Scatter Plot": return "google.visualization.ScatterChart";
    }
}

void removeLastChar(str) {
    str.subSequence(0, str.length() - 1)
    str
}

def getLineGraph() {
    def fullSizeStyle = "margin: 0; padding: 0; width: 100%; height: 100%; overflow: hidden";
    
    def html = """
    <html style="${fullSizeStyle}">
    <link rel='icon' href='https://www.shareicon.net/data/256x256/2015/09/07/97252_barometer_512x512.png' type='image/x-icon'/> 
    <link rel="apple-touch-icon" href="https://www.shareicon.net/data/256x256/2015/09/07/97252_barometer_512x512.png">
    <head>
      <script src="https://code.jquery.com/jquery-3.5.0.min.js" integrity="sha256-xNzN2a4ltkB44Mc/Jz3pT4iU1cmeR0FkXs4pru/JxaQ=" crossorigin="anonymous"></script>
      <script src="https://cdnjs.cloudflare.com/ajax/libs/svg.js/3.0.16/svg.min.js" integrity="sha256-MCvBrhCuX8GNt0gmv06kZ4jGIi1R2QNaSkadjRzinFs=" crossorigin="anonymous"></script>
      <script src="https://cdnjs.cloudflare.com/ajax/libs/moment.js/2.25.0/moment.min.js" integrity="sha256-imB/oMaNA0YvIkDkF5mINRWpuFPEGVCEkHy6rm2lAzA=" crossorigin="anonymous"></script>
      <script type="text/javascript" src="https://www.gstatic.com/charts/loader.js"></script>
      <script type="text/javascript">
google.charts.load('current', {'packages':['corechart']});

let options = [];
let subscriptions = {};
let graphData = {};

//stack for accumulating points to average
let stack = {};

let websocket;

class Loader {
    constructor() {
        this.elem = jQuery(jQuery(document.body).prepend(`
            <div class="loaderContainer">
                <div class="dotsContainer">
                    <div class="dot"></div>
                    <div class="dot"></div>
                    <div class="dot"></div>
                </div>
                <div class="text"></div>
            </div>
        `).children()[0]);
    }

    setText(text) {
        this.elem.find('.text').text(text);
    }

    remove() {
        this.elem.remove();
    }
}

function getOptions() {
    return jQuery.get("${state.localEndpointURL}getOptions/?access_token=${state.endpointSecret}", (data) => {
        options = data;
        console.log("Got Options");
        console.log(options);
    });
}

function getSubscriptions() {
    return jQuery.get("${state.localEndpointURL}getSubscriptions/?access_token=${state.endpointSecret}", (data) => {
        console.log("Got Subscriptions");
        console.log(data);
        subscriptions = data;
        
    });
}

function getGraphData() {
    return jQuery.get("${state.localEndpointURL}getData/?access_token=${state.endpointSecret}", (data) => {
        console.log("Got Graph Data");
        console.log(data);
        graphData = data;
    });
}

function parseEvent(event) {
    const now = new Date().getTime();
    let deviceId = event.deviceId;

    //only accept relevent events
    if(subscriptions.ids.includes(deviceId) && subscriptions.attributes[deviceId].includes(event.name)) {
        let value = event.value;
        let attribute = event.name;

        stack[deviceId][attribute].push({ date: now, value });
        
        //check the stack
        const graphEvents = graphData[deviceId][attribute];
        const stackEvents = stack[deviceId][attribute];
        const span = graphEvents[1].date - graphEvents[0].date;
        if(stackEvents[stackEvents.length - 1].date - graphEvents[graphEvents.length - 1].date >= span || (stackEvents.length > 1 && stackEvents[stackEvents.length - 1].date - stackEvents[0].date >= span)) {
            //push the stack
            graphData[deviceId][attribute].push(stack[deviceId][attribute].reduce((accum, it) =>  accum = { date: accum.date + it.date / stackEvents.length, value: accum.value + it.value / stackEvents.length }, { date: 0, value: 0.0 }));
            stack[deviceId][attribute] = [];
        }

        //update if we are realtime
        if(options.graphUpdateRate === 0) update();
    }
}

function update(callback) {
    //boot old data
    let min = new Date().getTime();
    min -= options.graphTimespan;

    Object.entries(graphData).forEach(([deviceId, attributes]) => {
        Object.entries(attributes).forEach(([attribute, events]) => {
            //filter old events
            graphData[deviceId][attribute] = events.filter(it => it.date > min);
        });
    });

    
    drawChart(callback);   
}

async function onLoad() {
    //append our css
    jQuery(document.head).append(`
        <style>
            .loaderContainer {
                position: fixed;
                z-index: 100;

                width: 100%;
                height: 100%;

                background-color: white;
                
                display: flex;
                flex-flow: column nowrap;
                justify-content: center;
                align-items: middle;
            }

            .dotsContainer {
                height: 60px;
                padding-bottom: 10px;

                display: flex;
                flex-flow: row nowrap;
                justify-content: center;
                align-items: flex-end;
            }

            @keyframes bounce {
                0% {
                    transform: translateY(0);
                }

                50% {
                    transform: translateY(-50px);
                }

                100% {
                    transform: translateY(0);
                }
            }

            .dot {
                box-sizing: border-box;

                margin: 0 25px;

                width: 10px;
                height: 10px;

                border: solid 5px black;
                border-radius: 5px;

                animation-name: bounce;
                animation-duration: 1s;
                animation-iteration-count: infinite;
            }

            .dot:nth-child(1) {
                animation-delay: 0ms;
            }

            .dot:nth-child(2) {
                animation-delay: 333ms;
            }

            .dot:nth-child(3) {
                animation-delay: 666ms;
            }

            .text {
                font-family: Arial;
                font-weight: 200;
                font-size: 2rem;
                text-align: center;
            }
        </style>
    `);

    let loader = new Loader();

    //first load
    loader.setText('Getting options (1/4)');
    await getOptions();
    loader.setText('Getting device data (2/4)');
    await getSubscriptions();
    loader.setText('Getting events (3/4)');
    await getGraphData();

    loader.setText('Drawing chart (4/4)');

    //create stack
    Object.entries(graphData).forEach(([deviceId, attrs]) => {
        stack[deviceId] = {};
        Object.keys(attrs).forEach(attr => {
            stack[deviceId][attr] = [];
        });
    })

    update(() => {
        //destroy loader when we are done with it
        loader.remove();
    });

    //start our update cycle
    if(options.graphUpdateRate !== -1) {
        //start websocket
        websocket = new WebSocket("ws://" + location.hostname + "/eventsocket");
        websocket.onopen = () => {
            console.log("WebSocket Opened!");
        }
        websocket.onmessage = (event) => {
            parseEvent(JSON.parse(event.data));
        }

        if(options.graphUpdateRate !== 0) {
            setInterval(() => {
                update();
            }, options.graphUpdateRate);
        }
    }

    //attach resize listener
    window.addEventListener("resize", () => {
        drawChart();
    });
}

function onBeforeUnload() {
    if(websocket) websocket.close();
}

function drawChart(callback) {
    let now = new Date().getTime();
    let min = now - options.graphTimespan;

    let dataTable = new google.visualization.DataTable();
    dataTable.addColumn({ label: 'Date', type: 'datetime' });

    let colNums = {};

    let i = 0;
    subscriptions.ids.forEach((deviceId) => {
        colNums[deviceId] = {};
        subscriptions.attributes[deviceId].forEach((attr) => {
            dataTable.addColumn({ label: subscriptions.labels[deviceId][attr].replace('%deviceName%', subscriptions.sensors[deviceId].displayName).replace('%attributeName%', attr), type: 'number' });
            colNums[deviceId][attr] = i++;
        });
    });

    const totalCols = i;

    let parsedGraphData = [];
    //map the graph data
    Object.entries(graphData).forEach(([deviceIndex, attributes]) => {
        Object.entries(attributes).forEach(([attribute, events]) => {
            events.forEach((event) => {
                //try to find an already existing date
                //let index = parsedGraphData.findIndex((val) => val[0] === event.date);
                //if(index !== -1) parsedGraphData[index][colNums[deviceIndex][attribute] + 1] = event.value;
                //else {
                    //else make a new entry
                    let newEntry = Array.apply(null, new Array(totalCols + 1));
                    newEntry[0] = event.date;
                    newEntry[colNums[deviceIndex][attribute] + 1] = event.value;
                    parsedGraphData.push(newEntry);
                //}
            });
            
        });
    });

    //map the stack
    Object.entries(stack).forEach(([deviceIndex, attributes]) => {
        Object.entries(attributes).forEach(([attribute, events]) => {
            if(events.length > 0) {
                const event = events.reduce((accum, it) =>  accum = { date: accum.date, value: accum.value + it.value / events.length }, { date: now, value: 0.0 });

                let newEntry = Array.apply(null, new Array(totalCols + 1));
                newEntry[0] = event.date;
                newEntry[colNums[deviceIndex][attribute] + 1] = event.value;
                parsedGraphData.push(newEntry);
            }
        });
    });

    parsedGraphData = parsedGraphData.map((it) => [ moment(it[0]).toDate(), ...it.slice(1).map((it) => parseFloat(it)) ]);

    parsedGraphData.forEach(it => {
        dataTable.addRow(it);
    });

    
    
    let graphOptions = Object.assign({}, options.graphOptions);

    graphOptions.hAxis = Object.assign(graphOptions.hAxis, { viewWindow: { min: moment(min).toDate(), max: moment(now).toDate() } });

    let chart = new ${drawType}(document.getElementById("timeline"));

    //if we have a callback
    if(callback) google.visualization.events.addListener(chart, 'ready', callback);

    chart.draw(dataTable, graphOptions);
}

google.charts.setOnLoadCallback(onLoad);
window.onBeforeUnload = onBeforeUnload;

        </script>
      </head>
      <body style="${fullSizeStyle}">
        <div id="timeline" style="${fullSizeStyle}" align="center"></div>
      </body>
       
    </html>
    """
    
    return html;
}

// Events come in Date format
def getDateStringEvent(date) {
    def dateObj = date
    def yyyy = dateObj.getYear() + 1900
    def MM = String.format("%02d", dateObj.getMonth()+1);
    def dd = String.format("%02d", dateObj.getDate());
    def HH = String.format("%02d", dateObj.getHours());
    def mm = String.format("%02d", dateObj.getMinutes());
    def ss = String.format("%02d", dateObj.getSeconds());
    def dateString = /$yyyy-$MM-$dd $HH:$mm:$ss.000/;
    dateString
}
    
def initializeAppEndpoint() {
    if (!state.endpoint) {
        try {
            def accessToken = createAccessToken()
            if (accessToken) {
                state.endpoint = getApiServerUrl()
                state.localEndpointURL = fullLocalApiServerUrl("")  
                state.remoteEndpointURL = fullApiServerUrl("")
                state.endpointSecret = accessToken
            }
        }
        catch(e) {
            state.endpoint = null
        }
    }
    return state.endpoint
}

//oauth endpoints
def getGraph() {
    render(contentType: "text/html", data: getLineGraph());      
}

def getDataMetrics() {
    def data;
    def then = new Date().getTime();
    data = getData();
    def now = new Date().getTime();
    logDebug("Command Time (ms): ${now - then}");
    return data;
}

def getData() {
    def timeline = buildData();
    return render(contentType: "text/json", data: JsonOutput.toJson(timeline));
}

def getOptions() {
    render(contentType: "text/json", data: JsonOutput.toJson(getChartOptions()));
}

def getSubscriptions() {
    def ids = [];
    def sensors_ = [:];
    def attributes = [:];
    def labels = [:];
    sensors.each {sensor->
        ids << sensor.idAsLong;
        
        //only take what we need
        sensors_[sensor.id] = [ id: sensor.id, idAsLong: sensor.idAsLong, displayName: sensor.displayName ];        
        attributes[sensor.id] = settings["attributes_${sensor.id}"];
        
        labels[sensor.id] = [:];
        settings["attributes_${sensor.id}"].each { attr ->
            labels[sensor.id][attr] = settings["graph_name_override_${sensor.id}_${attr}"];
        }
    }
    
    def obj = [
        ids: ids,
        sensors: sensors_,
        attributes: attributes, 
        labels : labels
    ]
    
    def subscriptions = obj;
    
    return render(contentType: "text/json", data: JsonOutput.toJson(subscriptions));
}

def getColorCode(code){
    
    ret = "#FFFFFF"
    switch (code){
        case 7:  ret = "#800000"; break;
        case 1:	    ret = "#FF0000"; break;
        case 6:	ret = "#FFA500"; break;	
        case 8:	ret = "#FFFF00"; break;	
        case 9:	ret = "#808000"; break;	
        case 2:	ret = "#008000"; break;	
        case 5:	ret = "#800080"; break;	
        case 4:	ret = "#FF00FF"; break;	
        case 10: ret = "#00FF00"; break;	
        case 11: ret = "#008080"; break;	
        case 12: ret = "#00FFFF"; break;	
        case 3:	ret = "#0000FF"; break;	
        case 13: ret = "#000080"; break;	
    }
    return ret;
}

