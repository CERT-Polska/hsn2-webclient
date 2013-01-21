#
# This Makefile is responsoble for build debian package.
# 4 variables must be provided:
#   HSN2_VER=2.X
#   BUILD_NUMBER=999
#   DEBIAN_DIST=(experimental|2.X)

DEBIAN_DIST=experimental
HSN2_COMPONENT=webclient

all:	hsn2-webclient

clean:	hsn2-webclient-clean

PKG=hsn2-$(HSN2_COMPONENT)_$(HSN2_VER)-$(BUILD_NUMBER)_all
hsn2-webclient: hsn2-webclient-clean
	mkdir -p $(PKG)/DEBIAN
	mkdir -p $(PKG)/etc/init.d
	mkdir -p $(PKG)/opt/hsn2/$(HSN2_COMPONENT)
	tar -zxf ../hsn2-webclient-distribution/target/hsn2-webclient-*.tar.gz -C $(PKG)/opt/hsn2/$(HSN2_COMPONENT)
	cp webclient.initd $(PKG)/etc/init.d/hsn2-$(HSN2_COMPONENT)
	cp control ${PKG}/DEBIAN
	sed -i "s/{VER}/${HSN2_VER}-${BUILD_NUMBER}/" $(PKG)/DEBIAN/control
	sed -i "s/{DEBIAN_DIST}/${DEBIAN_DIST}/" $(PKG)/DEBIAN/control
	fakeroot dpkg -b $(PKG)

hsn2-webclient-clean:
	rm -rf hsn2-$(HSN2_COMPONENT)*
