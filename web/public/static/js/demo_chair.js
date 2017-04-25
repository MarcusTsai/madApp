(function() {
    'use strict';

    madAPP.controller('demoChairController', demoChairController);

    function demoChairController($timeout) {
        var vm = this;
        vm.text = 'Firebase rulez!';

        var dbRef = firebase.database().ref();
        var appid = "-KY0VEKXfHvj_tbzTh4c";
        vm.app_config = null;
        var instances = null;
        var appRef = dbRef.child('apps').child(appid);
        var install_sensors = dbRef.child('install_sensors');
        var install_actuators = dbRef.child('install_actuators');

        vm.light_reading = null;
        vm.flash_on = null;

        appRef.once('value').then(function(snapshot) {
            vm.app_config = snapshot.val();
            instances = _.keys(vm.app_config.instances);
        });

        var app_idsRef = dbRef.child('app_ids');

        app_idsRef.orderByChild("app_id").equalTo(appid).on('child_added', function(data) {
            console.log('new_instance', data.key);
            console.log('instance data', data.val());
            var new_instance = data.key;
            var num_device = data.val().num_device;
            var install_sensorRef = install_sensors.child(new_instance);
            var install_actuatorRef = install_actuators.child(new_instance);
            var actuator_id = null;
            install_actuatorRef.child('speaker').once("value").then(function(snapshot) {
                actuator_id = _.keys(snapshot.val())[0];
                console.log('speaker', actuator_id);
            });

            var num_moved_device = 0;
            install_sensorRef.child('accelerometer').orderByChild("value").equalTo(true).on('child_added', function(snapshot) {
                $timeout(function() {
                    // var accelerometers = _.keys(snapshot.val());
                    num_moved_device += 1;
                    console.log(snapshot.val());
                    var msg = num_moved_device > 1 ? num_moved_device + 'guests have joined' : num_moved_device + 'guest has joined';
                    install_actuatorRef.child('speaker').child(actuator_id).child('sound_message').set(msg);
                    // install_actuatorRef.child('speaker').child(actuator_id).set(msg);

                    // if (num_moved_device == num_device - 1) {
                    //     install_actuatorRef.child('speaker').child(actuator_id).set('All Guests have joined');
                    // }
                }, 0);
            });
        });
    }
})();
