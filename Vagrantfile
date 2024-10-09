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
  
  (1..1).each do |i|
    config.vm.define "tcp-#{i}" do |node|
      node.vm.hostname = "tcp"
      node.vm.box = "testbed-node"
      node.vm.network "public_network", ip: "10.50.50.10#{i}"
    end
  end

  #config.vm.define "hmi" do |hmi|
  #  hmi.vm.hostname = "hmi"
  #  hmi.vm.box = "testbed-node"
  #  hmi.vm.network "public_network", ip: "10.50.50.200"
  #end
end
