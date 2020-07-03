
  Pod::Spec.new do |s|
    s.name = 'CapacitorPersistentNotification'
    s.version = '0.0.1'
    s.summary = 'A plugin to create a persistent notification in Android to allow continous background work.'
    s.license = 'MIT'
    s.homepage = 'capacitor-persistent-notification'
    s.author = 'Joshua Berlin'
    s.source = { :git => 'capacitor-persistent-notification', :tag => s.version.to_s }
    s.source_files = 'ios/Plugin/**/*.{swift,h,m,c,cc,mm,cpp}'
    s.ios.deployment_target  = '11.0'
    s.dependency 'Capacitor'
  end