

from distutils.core import setup

setup(name="gratia-gold",
      version="1.2",
      author="Srini Ramachandran",
      author_email="srini@fnal.gov",
      url="https://github.com/bbockelman/gratia-gold",
      description="Probe for synchronizing Gratia and GOLD",
      package_dir={"": "src"},
      packages=["gratia_gold"],

      scripts = ['src/gratia-gold'],

      data_files=[("/etc/cron.d", ["config/gratia-gold.cron"]),
            ("/etc/", ["config/gratia-gold.cfg"]),
            ("/etc/", ["config/gratia_gold_rules.csv"]),
            ("/etc/", ["config/gratia_gold_blacklist.csv"]),
            ("/etc/", ["config/gratia_gold_whitelist.csv"]),
            ("/etc/logrotate.d", ["config/gratia-gold.logrotate"]),
          ],

     )

