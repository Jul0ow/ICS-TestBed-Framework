# -*- mode: ruby -*-
# vi: set ft=ruby :

Vagrant.configure("2") do |config|
  config.vm.box_check_update = true

  config.vm.provider "vmware_desktop" do |vmware|
    vmware.memory = 512
    vmware.allowlist_verified = true
    vmware.gui = false
  end

  config.vm.provision "ansible_local" do |ansible|
    ansible.playbook = "playbook.yml"
    ansible.compatibility_mode = "2.0"
  end
  
  config.vm.define "plc-progression" do |node|
    node.vm.hostname = "plc-progression"
    node.vm.box = "testbed-node"
    node.vm.network "private_network", ip: "10.50.50.100"
  end

  config.vm.define "plc-brakes" do |node|
    node.vm.hostname = "plc-brakes"
    node.vm.box = "testbed-node"
    node.vm.network "private_network", ip: "10.50.50.101"
  end

  config.vm.define "plc-security" do |node|
      node.vm.hostname = "plc-security"
      node.vm.box = "testbed-node"
      node.vm.network "private_network", ip: "10.50.50.102"
    end

  config.vm.define "plc-lights" do |node|
      node.vm.hostname = "plc-lights"
      node.vm.box = "testbed-node"
      node.vm.network "private_network", ip: "10.50.50.103"
  end

  config.vm.define "hmi" do |hmi|
    hmi.vm.hostname = "hmi"
    hmi.vm.box = "testbed-node"
    hmi.vm.network "private_network", ip: "10.50.50.200"
  end
end
