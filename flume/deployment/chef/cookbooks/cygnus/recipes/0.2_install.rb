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
# frb@tid.es

node.default[:cygnus][:version] = "0.2"

include_recipe "cygnus::install_dependencies"
include_recipe "cygnus::install_jdk"
include_recipe "cygnus::install_maven"
include_recipe "cygnus::install_flume"
include_recipe "cygnus::install_cygnus"
include_recipe "cygnus::start_cygnus"