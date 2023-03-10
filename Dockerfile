# Copyright (C) 2023 Lucas Nishimura <lucas.nishimura@gmail.com>
#
# This program is free software; you can redistribute it and/or
# modify it under the terms of the GNU General Public License
# as published by the Free Software Foundation; either version 2
# of the License, or (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program; if not, write to the Free Software
# Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
FROM debian:latest

# Atualiza e instala o Java
RUN apt update -y
RUN apt upgrade -y
RUN apt install openjdk-17-jdk xfsprogs -y
RUN mkdir -p /app/inventory-manager/ssl
RUN mkdir -p /app/inventory-manager/samples 
RUN mkdir -p /app/inventory-manager/schema
RUN ln -s /app/inventory-manager/schema /app/inventory-manager/samples

WORKDIR /app/inventory-manager
COPY target/*.jar .
COPY run-java.sh .
ADD ssl /app/inventory-manager/ssl

RUN chmod +x run-java.sh
ENTRYPOINT ["./run-java.sh"]
