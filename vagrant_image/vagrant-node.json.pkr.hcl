packer {
  required_plugins {
    ansible = {
      source  = "github.com/hashicorp/ansible"
      version = "~> 1"
    }
    vagrant = {
      source  = "github.com/hashicorp/vagrant"
      version = "~> 1"
    }
    vmware = {
      source  = "github.com/hashicorp/vmware"
      version = "~> 1"
    }
  }
}

source "vmware-iso" "autogenerated_1" {
  boot_command      = ["<esc><wait>", "<esc><wait>", "<enter><wait>", "/install/vmlinuz<wait>", " auto<wait>", " console-setup/ask_detect=false<wait>", " console-setup/layoutcode=gb<wait>", " console-setup/modelcode=pc105<wait>", " debconf/frontend=noninteractive<wait>", " debian-installer=en_GB<wait>", " fb=false<wait>", " initrd=/install/initrd.gz<wait>", " kbd-chooser/method=gb<wait>", " keyboard-configuration/layout=gb<wait>", " keyboard-configuration/variant=gb<wait>", " locale=en_GB<wait>", " netcfg/get_domain=vm<wait>", " netcfg/get_hostname=vagrant<wait>", " grub-installer/bootdev=/dev/sda<wait>", " noapic<wait>", " preseed/url=http://{{ .HTTPIP }}:{{ .HTTPPort }}/preseed.cfg<wait>", " -- <wait>", "<enter><wait>"]
  guest_os_type     = "ubuntu-64"
  headless          = true
  http_directory    = "./"
  iso_checksum      = "f5cbb8104348f0097a8e513b10173a07dbc6684595e331cb06f93f385d0aecf6"
  iso_url           = "https://cdimage.ubuntu.com/ubuntu/releases/18.04.5/release/ubuntu-18.04.6-server-amd64.iso"
  shutdown_command  = "echo 'vagrant'|sudo -S /sbin/halt -h -p"
  ssh_password      = "vagrant"
  ssh_username      = "vagrant"
  vm_name           = "packer-testbed-node-x86_64"
}

build {
  sources = ["source.vmware-iso.autogenerated_1"]

  provisioner "shell" {
    execute_command = "echo 'vagrant' | {{ .Vars }} sudo -S -E bash '{{ .Path }}'"
    script          = "scripts/ansible.sh"
  }

  provisioner "shell" {
    execute_command = "echo 'vagrant' | {{ .Vars }} sudo -S -E bash '{{ .Path }}'"
    script          = "scripts/setup.sh"
  }

  provisioner "ansible-local" {
    playbook_file = "vagrant.yml"
  }

  provisioner "shell" {
    execute_command = "echo 'vagrant' | {{ .Vars }} sudo -S -E bash '{{ .Path }}'"
    script          = "scripts/cleanup.sh"
  }

  post-processor "vagrant" {
    output = "vagrant.box"
  }
}
