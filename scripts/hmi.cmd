hmi
add-plc --name progression --ipAddr 10.50.50.100 --type progression
add-plc --name brakes --ipAddr 10.50.50.101 --type brake
add-plc --name security --ipAddr 10.50.50.102 --type security
add-plc --name lights --ipAddr 10.50.50.103 --type lights
add-plc --name engine --ipAddr 10.50.50.104 --type engine
routine --BrakesName brakes --EngineName engine --LightsName lights --ProgressionName progression --SecurityName security
