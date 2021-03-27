$(function() {
    getVoices().then(voices => {
        if (voices !== null) {
            let dropdown = $("#voiceSelector");
            voices.sort((a,b) => {
                if (a.gender !== b.gender) {
                    return a.gender > b.gender ? 1 : -1;
                } else {
                    return a.name > b.name ? 1 : -1;
                }
            });
            voices.forEach((voice, i) => {
                let gender = voice.gender.toLowerCase();
                gender = gender.charAt(0).toUpperCase() + gender.substring(1);
                dropdown.append(`<option value=${voice.name}>Voice ${i} - ${gender}</option>`)
            });
            $(dropdown.firstChild).attr("selected");

            dropdown.on("change", function() {
                playSample(this.value);
            });
        } else {
            alert("Error contacting server!");
        }
    });

    $("#submit").click(function() {
        let voiceName = $("#voiceSelector").val();
        let userName = $("#name").val();
        let message = $("#message").val();
        sayText(voiceName, userName, message);
    });
});