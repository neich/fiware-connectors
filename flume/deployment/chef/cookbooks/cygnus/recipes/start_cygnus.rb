#! /bin/bash
# Copyright 2014 Telefonica Investigacion y Desarrollo, S.A.U
#
# This file is part of fiware-connectors
#
# fiware-connectors is free software: you can redistribute it and/or
# modify it under the terms of the GNU Affero General Public License as
# published by the Free Software Foundation, either version 3 of the
# License, or (at your option) any later version.
#
# fiware-connectors is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero
# General Public License for more details.
#
# You should have received a copy of the GNU Affero General Public License
# along with Orion Context Broker. If not, see http://www.gnu.org/licenses/.
#
# For those usages not covered by this license please contact with
# francisco.romerobueno@telefonica.com

# Start Cygnus.
execute "start_cygnus" do
        user "root"
        command "nohup #{node[:flume][:path]}/apache-flume-#{node[:flume][:version]}-bin/bin/flume-ng agent --conf #{node[:flume][:path]}/apache-flume-#{node[:flume][:version]}-bin/conf -f #{node[:flume][:path]}/apache-flume-#{node[:flume][:version]}-bin/conf/cygnus.conf -n orionagent -Dflume.root.logger=INFO,LOGFILE &"
        action :run
end
